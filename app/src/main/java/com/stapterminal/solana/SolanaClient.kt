package com.stapterminal.solana

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.sol4k.Base58
import org.sol4k.Connection
import org.sol4k.PublicKey
import org.sol4k.RpcUrl
import org.sol4k.TransactionMessage
import org.sol4k.VersionedTransaction
import org.sol4k.instruction.SplTransferInstruction
import java.math.BigDecimal
import java.math.RoundingMode

enum class SolanaNetwork {
    MAINNET,
    DEVNET,
}

/**
 * Wrapper around the Solana JSON-RPC API for checking balances and sending USDC.
 *
 * Set [merchantKey] before use. All network calls are suspend functions and switch to
 * [Dispatchers.IO] internally, so they are safe to call directly from a Composable's
 * coroutine scope or a ViewModel.
 */
class SolanaClient(network: SolanaNetwork) {

    /**
     * Base58-encoded wallet address of this terminal, used as the default account for
     * balance checks and as the recipient of USDC transfers. The terminal never holds a
     * private key - the payer's key lives on the STap wallet and signs over NFC.
     */
    var merchantKey: String = "5sG3pWuuRXvrxo4ca2VH87yHBQuEwTqAbXfqt3kFFgrz"

    private val connection = Connection(
        when (network) {
            SolanaNetwork.MAINNET -> RpcUrl.MAINNNET
            SolanaNetwork.DEVNET -> RpcUrl.DEVNET
        },
    )

    private val usdcMint = PublicKey(
        when (network) {
            SolanaNetwork.MAINNET -> USDC_MINT_MAINNET
            SolanaNetwork.DEVNET -> USDC_MINT_DEVNET
        },
    )

    /** SOL balance of [address] (defaults to [merchantKey]), denominated in SOL. */
    suspend fun getSolBalance(address: String = merchantKey): BigDecimal = withContext(Dispatchers.IO) {
        BigDecimal(connection.getBalance(PublicKey(address))).movePointLeft(LAMPORTS_PER_SOL_DECIMALS)
    }

    /** USDC balance of [address] (defaults to [merchantKey]). Returns zero if it has no USDC token account yet. */
    suspend fun getUsdcBalance(address: String = merchantKey): BigDecimal = withContext(Dispatchers.IO) {
        val tokenAccount = PublicKey.findProgramDerivedAddress(PublicKey(address), usdcMint).publicKey
        if (connection.getAccountInfo(tokenAccount) == null) {
            return@withContext BigDecimal.ZERO
        }
        BigDecimal(connection.getTokenAccountBalance(tokenAccount).uiAmount)
    }

    /**
     * Builds the unsigned message for a transfer of [amount] USDC from [fromAddress] to
     * [toAddress], with a freshly fetched blockhash. [fromAddress] pays the transfer and
     * the network fee, and must sign the returned message's [TransactionMessage.serialize]
     * bytes before it can be submitted with [submitSignedUsdcTransfer].
     */
    suspend fun buildUsdcTransferMessage(
        fromAddress: String,
        toAddress: String,
        amount: BigDecimal,
    ): TransactionMessage = withContext(Dispatchers.IO) {
        val owner = PublicKey(fromAddress)
        val fromTokenAccount = PublicKey.findProgramDerivedAddress(owner, usdcMint).publicKey
        val toTokenAccount = PublicKey.findProgramDerivedAddress(PublicKey(toAddress), usdcMint).publicKey
        val rawAmount = amount.movePointRight(USDC_DECIMALS).setScale(0, RoundingMode.DOWN).longValueExact()

        val instruction = SplTransferInstruction(
            from = fromTokenAccount,
            to = toTokenAccount,
            mint = usdcMint,
            owner = owner,
            amount = rawAmount,
            decimals = USDC_DECIMALS,
        )
        TransactionMessage.newMessage(owner, connection.getLatestBlockhash(), instruction)
    }

    /**
     * Attaches the payer's raw ed25519 [signature] (as produced by signing
     * `message.serialize()`) to [message] and submits the resulting transaction. Returns
     * the transaction signature.
     */
    suspend fun submitSignedUsdcTransfer(message: TransactionMessage, signature: ByteArray): String =
        withContext(Dispatchers.IO) {
            val transaction = VersionedTransaction(message)
            transaction.addSignature(Base58.encode(signature))
            connection.sendTransaction(transaction)
        }

    private companion object {
        const val LAMPORTS_PER_SOL_DECIMALS = 9
        const val USDC_DECIMALS = 6

        // Circle's official USDC mint addresses.
        const val USDC_MINT_MAINNET = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"
        const val USDC_MINT_DEVNET = "4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU"
    }
}
