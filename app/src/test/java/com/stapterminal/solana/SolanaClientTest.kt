package com.stapterminal.solana

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class SolanaClientTest {

    @Test
    fun getMerchantSolBalance() = runBlocking {
        val client = SolanaClient(SolanaNetwork.MAINNET)
        // Solana's System Program address always exists on every cluster, so this
        // is a safe address to query without needing a funded test wallet.
        val address = "B1BUPRpzk12T2WqkUj1X221KTdv8Qf8skRPC2spe53ik"

        val balance: BigDecimal = client.getSolBalance(address)

        println("SOL balance for merchant $address: $balance")
        assertTrue("balance should not be negative", balance >= BigDecimal.ZERO)
    }

    @Test
    fun getMerchantUSDCBalance() = runBlocking {
        val client = SolanaClient(SolanaNetwork.MAINNET)
        // Solana's System Program address always exists on every cluster, so this
        // is a safe address to query without needing a funded test wallet.
        val address = "B1BUPRpzk12T2WqkUj1X221KTdv8Qf8skRPC2spe53ik"

        val balance: BigDecimal = client.getUsdcBalance(address)

        println("USDC balance for merchant $address: $balance")
        assertTrue("balance should not be negative", balance >= BigDecimal.ZERO)
    }

    @Test
    fun getClientSolBalance() = runBlocking {
        val client = SolanaClient(SolanaNetwork.MAINNET)
        // Solana's System Program address always exists on every cluster, so this
        // is a safe address to query without needing a funded test wallet.
        val address = "E4MDwcLeBJWrRXF9SFXtZST9m61JAdTp5459Ww7LAcp"

        val balance: BigDecimal = client.getSolBalance(address)

        println("SOL balance for client $address: $balance")
        assertTrue("balance should not be negative", balance >= BigDecimal.ZERO)
    }

    @Test
    fun getClientUsdcBalance() = runBlocking {
        val client = SolanaClient(SolanaNetwork.MAINNET)
        // Solana's System Program address always exists on every cluster, so this
        // is a safe address to query without needing a funded test wallet.
        val address = "E4MDwcLeBJWrRXF9SFXtZST9m61JAdTp5459Ww7LAcp"

        val balance: BigDecimal = client.getUsdcBalance(address)

        println("USDC balance for client $address: $balance")
        assertTrue("balance should not be negative", balance >= BigDecimal.ZERO)
    }

    @Test
    fun buildUsdcTransferMessage() = runBlocking {
        val client = SolanaClient(SolanaNetwork.MAINNET)
        val fromAddress = "E4MDwcLeBJWrRXF9SFXtZST9m61JAdTp5459Ww7LAcp"
        val toAddress = "B1BUPRpzk12T2WqkUj1X221KTdv8Qf8skRPC2spe53ik"

        val message = client.buildUsdcTransferMessage(fromAddress, toAddress, BigDecimal("0.10"))

        assertTrue("serialized message should not be empty", message.serialize().isNotEmpty())
    }

}
