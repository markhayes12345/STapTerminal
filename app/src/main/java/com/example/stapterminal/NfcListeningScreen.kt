package com.example.stapterminal

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stapterminal.ui.theme.STapTerminalTheme

@Composable
fun NfcListeningScreen(
    modifier: Modifier = Modifier,
    onCancel: () -> Unit = {},
    onTagDetected: (Tag) -> Unit = {}
) {
    val context = LocalContext.current
    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }

    DisposableEffect(Unit) {
        val activity = context as? Activity
        val callback = NfcAdapter.ReaderCallback { tag -> onTagDetected(tag) }

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
            else -> "Waiting for card or device"
        }
        Text(text = statusText, fontSize = 20.sp, fontWeight = FontWeight.Medium)

        Spacer(8.dp)

        if (nfcAdapter != null && !nfcAdapter.isEnabled) {
            Text(text = "Turn on NFC to continue", fontSize = 14.sp)
            Spacer(16.dp)
            Button(onClick = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }) {
                Text("Open NFC settings")
            }
            Spacer(16.dp)
        } else {
            Text(text = "Hold the card or device near the back of the phone", fontSize = 14.sp)
            Spacer(32.dp)
        }

        OutlinedButton(onClick = onCancel) {
            Text("Cancel")
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
        NfcListeningScreen()
    }
}
