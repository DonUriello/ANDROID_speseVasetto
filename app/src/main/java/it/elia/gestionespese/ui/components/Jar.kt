package it.elia.gestionespese.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlin.math.min

private val Glass = Color(0xFFCFE3E8)
private val GlassEdge = Color(0xFF7FA6B1)
private val Lid = Color(0xFF8A6440)
private val CoinShadow = Color(0xFFB07F16)
private val CoinFace = Color(0xFFF0C24F)
private val CoinEdge = Color(0xFFC9961F)

/**
 * Vasetto di vetro con [layers] strati di monete (0–10), come `jarSvg` del prototipo.
 * Disegnato in un riquadro logico 100×150.
 */
@Composable
fun Jar(layers: Int, modifier: Modifier = Modifier) {
    Canvas(modifier.size(64.dp, 96.dp)) {
        val s = min(size.width / 100f, size.height / 150f)
        withTransform({
            translate((size.width - 100f * s) / 2f, (size.height - 150f * s) / 2f)
            scale(s, s, pivot = Offset.Zero)
        }) {
            drawJar(layers.coerceIn(0, 10))
        }
    }
}

private fun DrawScope.drawJar(layers: Int) {
    drawRoundRect(Glass.copy(alpha = 0.35f), Offset(10f, 32f), Size(80f, 110f), CornerRadius(15f))
    val inside = Path().apply { addRoundRect(RoundRect(12f, 34f, 88f, 140f, CornerRadius(13f))) }
    clipPath(inside) {
        for (l in 0 until layers) {
            val y = 132f - l * 10.4f
            var x = 5f + (l % 2) * 9f
            while (x <= 92f) {
                drawOval(CoinShadow, Offset(x - 10.5f, y + 2.2f - 4.4f), Size(21f, 8.8f))
                drawOval(CoinFace, Offset(x - 10.5f, y - 4.4f), Size(21f, 8.8f))
                drawOval(CoinEdge, Offset(x - 10.5f, y - 4.4f), Size(21f, 8.8f), style = Stroke(0.6f))
                x += 18f
            }
        }
    }
    drawRoundRect(GlassEdge, Offset(10f, 32f), Size(80f, 110f), CornerRadius(15f), style = Stroke(2.5f))
    drawRoundRect(Color.White.copy(alpha = 0.55f), Offset(18f, 42f), Size(5f, 70f), CornerRadius(2.5f))
    drawRoundRect(Glass.copy(alpha = 0.35f), Offset(22f, 22f), Size(56f, 12f), CornerRadius(3f))
    drawRoundRect(GlassEdge, Offset(22f, 22f), Size(56f, 12f), CornerRadius(3f), style = Stroke(2.5f))
    drawRoundRect(Lid, Offset(18f, 10f), Size(64f, 14f), CornerRadius(4f))
}
