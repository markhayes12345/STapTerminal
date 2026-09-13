package com.stapterminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stapterminal.ui.theme.STapTerminalTheme

private val DIGIT_ROWS = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9")
)

private fun formatAmount(digits: String): String {
    val padded = digits.padStart(3, '0')
    val decimalPart = padded.takeLast(2)
    val integerPart = padded.dropLast(2).trimStart('0').ifEmpty { "0" }
    return "$integerPart.$decimalPart"
}

@Composable
fun KeyPadScreen(
    modifier: Modifier = Modifier,
    onAccept: (String) -> Unit = {},
    onCancel: () -> Unit = {}
) {
    var digits by remember { mutableStateOf("0") }

    fun appendDigit(digit: String) {
        digits = (if (digits == "0") "" else digits) + digit
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "€${formatAmount(digits)}",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold
        )

        DIGIT_ROWS.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { digit ->
                    Button(
                        modifier = Modifier.weight(1f).aspectRatio(1f).padding(4.dp),
                        shape = RoundedCornerShape(12.dp),
                        onClick = { appendDigit(digit) }
                    ) {
                        Text(text = digit, fontSize = 24.sp)
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.weight(1f))
            Button(
                modifier = Modifier.weight(1f).aspectRatio(1f).padding(4.dp),
                shape = RoundedCornerShape(12.dp),
                onClick = { appendDigit("0") }
            ) {
                Text(text = "0", fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                modifier = Modifier.weight(1f).aspectRatio(1f).padding(4.dp),
                shape = RoundedCornerShape(12.dp),
                onClick = {
                    digits = if (digits.length <= 1) "0" else digits.dropLast(1)
                }
            ) {
                Text(text = "⌫", fontSize = 24.sp)
            }
            Button(
                modifier = Modifier.weight(1f).aspectRatio(1f).padding(4.dp),
                shape = RoundedCornerShape(12.dp),
                onClick = {
                    digits = "0"
                    onCancel()
                }
            ) {
                Text(text = "✕", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            }
            Button(
                modifier = Modifier.weight(1f).aspectRatio(1f).padding(4.dp),
                shape = RoundedCornerShape(12.dp),
                onClick = { onAccept(formatAmount(digits)) }
            ) {
                Text(text = "✓", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 24.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun KeyPadScreenPreview() {
    STapTerminalTheme {
        KeyPadScreen()
    }
}
