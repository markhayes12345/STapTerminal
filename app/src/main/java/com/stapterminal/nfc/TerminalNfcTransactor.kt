package com.stapterminal.nfc

import android.nfc.Tag
import android.nfc.tech.IsoDep
import com.stapterminal.solana.SolanaClient
import java.io.IOException
import java.math.BigDecimal

/** UI-facing state of an in-progress (or completed) tap-to-pay exchange. */
sealed interface PaymentState {
    data object WaitingForCard : PaymentState
    data object CardDetected : PaymentState
    data object AwaitingSignature : PaymentState
    data object SubmittingTransaction : PaymentState
    data class Success(val signature: String) : PaymentState
    data class Failure(val reason: String) : PaymentState
}

/**
 * Drives the STap payment protocol over a freshly detected [Tag]: asks the wallet who it
 * is, builds the unsigned USDC transfer, has the wallet sign it, submits it to Solana, and
 * reports the outcome back to the wallet.
 */
class TerminalNfcTransactor(private val solanaClient: SolanaClient) {

    suspend fun processTag(tag: Tag, amount: BigDecimal, onStateChange: (PaymentState) -> Unit) {
        val isoDep = IsoDep.get(tag)
        if (isoDep == null) {
            onStateChange(PaymentState.Failure("This device doesn't support STap payments"))
            return
        }

        try {
            isoDep.connect()
            isoDep.timeout = 8000

            val selectResponse = isoDep.transceive(StapNfcProtocol.buildSelectApdu())
            if (!selectResponse.isSuccessSw()) {
                onStateChange(PaymentState.Failure("Could not connect to STap wallet"))
                return
            }
            val fromAddress = String(selectResponse.responseData(), Charsets.US_ASCII)
            if (fromAddress.isBlank()) {
                onStateChange(PaymentState.Failure("STap wallet is locked"))
                return
            }
            onStateChange(PaymentState.CardDetected)

            val message = try {
                solanaClient.buildUsdcTransferMessage(fromAddress, solanaClient.publicKey, amount)
            } catch (e: Exception) {
                onStateChange(PaymentState.Failure("Could not prepare transaction: ${e.message}"))
                return
            }

            onStateChange(PaymentState.AwaitingSignature)

            val signature = sendMessageForSignature(isoDep, message.serialize())
            if (signature == null || signature.size != 64) {
                onStateChange(PaymentState.Failure("Did not receive a valid signature from STap wallet"))
                return
            }

            onStateChange(PaymentState.SubmittingTransaction)

            val txSignature = try {
                solanaClient.submitSignedUsdcTransfer(message, signature)
            } catch (e: Exception) {
                notifyStatus(isoDep, success = false, message = e.message ?: "Transaction failed")
                onStateChange(PaymentState.Failure(e.message ?: "Transaction failed"))
                return
            }

            notifyStatus(isoDep, success = true, message = txSignature)
            onStateChange(PaymentState.Success(txSignature))
        } catch (e: IOException) {
            onStateChange(PaymentState.Failure("Lost connection to STap wallet"))
        } finally {
            runCatching { isoDep.close() }
        }
    }

    /** Sends [messageBytes] to the wallet in chunks, returning the signature from the last chunk's response. */
    private fun sendMessageForSignature(isoDep: IsoDep, messageBytes: ByteArray): ByteArray? {
        val chunkSize = minOf(StapNfcProtocol.DEFAULT_CHUNK_SIZE, isoDep.maxTransceiveLength - 16)
            .coerceAtLeast(1)
        val chunks = if (messageBytes.isEmpty()) {
            listOf(messageBytes)
        } else {
            messageBytes.toList().chunked(chunkSize).map { it.toByteArray() }
        }

        var signature: ByteArray? = null
        for (index in chunks.indices) {
            val isLast = index == chunks.lastIndex
            val response = isoDep.transceive(StapNfcProtocol.buildChunkApdu(index, isLast, chunks[index]))
            if (!response.isSuccessSw()) return null
            if (isLast) signature = response.responseData()
        }
        return signature
    }

    private fun notifyStatus(isoDep: IsoDep, success: Boolean, message: String) {
        runCatching {
            val truncated = message.toByteArray(Charsets.UTF_8).take(StapNfcProtocol.STATUS_MESSAGE_MAX_BYTES).toByteArray()
            isoDep.transceive(StapNfcProtocol.buildStatusApdu(success, truncated))
        }
    }
}
