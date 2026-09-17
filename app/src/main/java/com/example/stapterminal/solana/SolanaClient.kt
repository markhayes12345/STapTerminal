package com.example.stapterminal.solana

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.sol4k.Base58
import org.sol4k.Connection
import org.sol4k.Keypair
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
 * Set [publicKey] (and [privateKey] for [transferUsdc]) before use. All network calls
 * are suspend functions and switch to [Dispatchers.IO] internally, so they are safe to
 * call directly from a Composable's coroutine scope or a ViewModel.
 */
class SolanaClient(network: SolanaNetwork) {

    /** Base58-encoded wallet address, used as the default account and, for transfers, the fee payer. */
    // this is the client public key
    // var publicKey: String = "E4MDwcLeBJWrRXF9SFXtZST9m61JAdTp5459Ww7LAcp"
    var publicKey: String = "AgWkfRL1xL2U1XUNXE35SHcczUBV2wStZti7kBNGLkDQ"

    /** Base58-encoded secret key matching [publicKey]. Only required for [transferUsdc]. */
    // this is the client private key
    var privateKey: String = "TODO"

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

    /** SOL balance of [address] (defaults to [publicKey]), denominated in SOL. */
    suspend fun getSolBalance(address: String = publicKey): BigDecimal = withContext(Dispatchers.IO) {
        BigDecimal(connection.getBalance(PublicKey(address))).movePointLeft(LAMPORTS_PER_SOL_DECIMALS)
    }

    /** USDC balance of [address] (defaults to [publicKey]). Returns zero if it has no USDC token account yet. */
    suspend fun getUsdcBalance(address: String = publicKey): BigDecimal = withContext(Dispatchers.IO) {
        val tokenAccount = PublicKey.findProgramDerivedAddress(PublicKey(address), usdcMint).publicKey
        if (connection.getAccountInfo(tokenAccount) == null) {
            return@withContext BigDecimal.ZERO
        }
        BigDecimal(connection.getTokenAccountBalance(tokenAccount).uiAmount)
    }

    /**
     * Transfers [amount] USDC from [publicKey] to [toAddress]. Requires [publicKey] and
     * [privateKey] to be set. Returns the transaction signature.
     */
    suspend fun transferUsdc(toAddress: String, amount: BigDecimal): String = withContext(Dispatchers.IO) {
        check(publicKey.isNotBlank()) { "publicKey has not been set" }
        check(privateKey.isNotBlank()) { "privateKey has not been set" }

        val owner = PublicKey(publicKey)
        val signer = Keypair.fromSecretKey(Base58.decode(privateKey))
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
        val message = TransactionMessage.newMessage(owner, connection.getLatestBlockhash(), instruction)
        val transaction = VersionedTransaction(message)
        transaction.sign(signer)

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
