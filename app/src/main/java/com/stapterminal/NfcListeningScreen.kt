package com.stapterminal

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stapterminal.nfc.PaymentState
import com.stapterminal.nfc.TerminalNfcTransactor
import com.stapterminal.solana.SolanaClient
import com.stapterminal.solana.SolanaNetwork
import com.stapterminal.ui.theme.STapTerminalTheme
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun NfcListeningScreen(
    modifier: Modifier = Modifier,
    amount: String,
    solanaClient: SolanaClient,
    onCancel: () -> Unit = {},
    onFinished: () -> Unit = {}
) {
    val context = LocalContext.current
    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }
    val scope = rememberCoroutineScope()
    val transactor = remember(solanaClient) { TerminalNfcTransactor(solanaClient) }
    var paymentState by remember { mutableStateOf<PaymentState>(PaymentState.WaitingForCard) }

    DisposableEffect(Unit) {
        val activity = context as? Activity
        val isProcessingTag = AtomicBoolean(false)
        val callback = NfcAdapter.ReaderCallback { tag ->
            if (isProcessingTag.compareAndSet(false, true)) {
                scope.launch(Dispatchers.IO) {
                    try {
                        transactor.processTag(tag, BigDecimal(amount)) { state ->
                            scope.launch { paymentState = state }
                        }
                    } finally {
                        isProcessingTag.set(false)
                    }
                }
            }
        }

        if (activity != null && nfcAdapter != null && nfcAdapter.isEnabled) {
            val options = Bundle().apply {
                putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)
            }
            nfcAdapter.enableReaderMode(
                activity,
                callback,
                NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                options
            )
        }

        onDispose {
            if (activity != null && nfcAdapter != null) {
                nfcAdapter.disableReaderMode(activity)
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PulsingContactlessIcon(modifier = Modifier.size(120.dp))

        Spacer(24.dp)

        val statusText = when {
            nfcAdapter == null -> "This device doesn't support NFC"
            !nfcAdapter.isEnabled -> "NFC is turned off"
            else -> when (val state = paymentState) {
                PaymentState.WaitingForCard -> "Waiting for card or device"
                PaymentState.CardDetected -> "STap wallet detected"
                PaymentState.AwaitingSignature -> "Waiting for wallet to sign…"
                PaymentState.SubmittingTransaction -> "Submitting payment…"
                is PaymentState.Success -> "Payment successful"
                is PaymentState.Failure -> "Payment failed"
            }
        }
        Text(text = statusText, fontSize = 20.sp, fontWeight = FontWeight.Medium)

        Spacer(8.dp)

        when (val state = paymentState) {
            is PaymentState.Success -> {
                Text(text = "Tx: ${state.signature}", fontSize = 12.sp, textAlign = TextAlign.Center)
                Spacer(24.dp)
                Button(onClick = onFinished) {
                    Text("Done")
                }
            }

            is PaymentState.Failure -> {
                Text(text = state.reason, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(16.dp)
                Button(onClick = { paymentState = PaymentState.WaitingForCard }) {
                    Text("Try again")
                }
                Spacer(8.dp)
                OutlinedButton(onClick = onCancel) {
                    Text("Cancel")
                }
            }

            else -> {
                if (nfcAdapter != null && !nfcAdapter.isEnabled) {
                    Text(text = "Turn on NFC to continue", fontSize = 14.sp)
                    Spacer(16.dp)
                    Button(onClick = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }) {
                        Text("Open NFC settings")
                    }
                    Spacer(16.dp)
                } else {
                    Text(text = "Hold the STap wallet near the back of the phone", fontSize = 14.sp)
                    Spacer(32.dp)
                }
                OutlinedButton(onClick = onCancel) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun Spacer(height: androidx.compose.ui.unit.Dp) {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = height))
}

@Composable
private fun PulsingContactlessIcon(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "nfc-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nfc-pulse-scale"
    )

    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.09f
        val center = Offset(size.width * 0.38f, size.height * 0.5f)
        val radii = listOf(0.16f, 0.28f, 0.40f).map { it * size.minDimension * pulse }
        radii.forEach { r ->
            drawArc(
                color = color,
                startAngle = -50f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(center.x - r, center.y - r),
                size = Size(r * 2, r * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NfcListeningScreenPreview() {
    STapTerminalTheme {
        NfcListeningScreen(amount = "12.34", solanaClient = SolanaClient(SolanaNetwork.MAINNET))
    }
}
