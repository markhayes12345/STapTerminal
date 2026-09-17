package com.stapterminal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.stapterminal.solana.SolanaClient
import com.stapterminal.solana.SolanaNetwork
import com.stapterminal.ui.theme.STapTerminalTheme

private sealed interface Screen {
    data object KeyPad : Screen
    data class NfcListening(val amount: String) : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            STapTerminalTheme {
                var screen by remember { mutableStateOf<Screen>(Screen.KeyPad) }
                val solanaClient = remember { SolanaClient(SolanaNetwork.MAINNET) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (val current = screen) {
                        is Screen.KeyPad -> KeyPadScreen(
                            modifier = Modifier.padding(innerPadding),
                            onAccept = { amount -> screen = Screen.NfcListening(amount) }
                        )

                        is Screen.NfcListening -> NfcListeningScreen(
                            modifier = Modifier.padding(innerPadding),
                            amount = current.amount,
                            solanaClient = solanaClient,
                            onCancel = { screen = Screen.KeyPad },
                            onFinished = { screen = Screen.KeyPad }
                        )
                    }
                }
            }
        }
    }
}