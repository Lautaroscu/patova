package ar.com.patova.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/**
 * Silueta del mascota "patovica" (cabeza rapada + anteojos + brazos cruzados),
 * la misma identidad del ícono de la app y del landing, dibujada en Canvas para
 * poder tintarla del color que corresponda (ej. sobre un badge lima o rojo).
 *
 * El diseño está pensado en un lienzo virtual de 40x40 y se escala al tamaño real.
 */
@Composable
fun PatovaMascot(
    modifier: Modifier = Modifier,
    color: Color
) {
    Canvas(modifier = modifier) {
        val scale = size.minDimension / 40f

        fun p(x: Float, y: Float) = Offset(x * scale, y * scale)

        // Cabeza
        drawCircle(
            color = color,
            radius = 8f * scale,
            center = p(20f, 13f)
        )

        // Anteojos (banda oscura sobre la cabeza, look "patovica")
        val glasses = Path().apply {
            moveTo(p(10f, 10f).x, p(10f, 10f).y)
            lineTo(p(30f, 10f).x, p(30f, 10f).y)
            lineTo(p(30f, 15f).x, p(30f, 15f).y)
            lineTo(p(10f, 15f).x, p(10f, 15f).y)
            close()
        }
        drawPath(path = glasses, color = color)

        // Hombros + brazos cruzados
        val body = Path().apply {
            moveTo(p(8f, 34f).x, p(8f, 34f).y)
            cubicTo(
                p(8f, 24f).x, p(8f, 24f).y,
                p(14f, 21f).x, p(14f, 21f).y,
                p(20f, 21f).x, p(20f, 21f).y
            )
            cubicTo(
                p(26f, 21f).x, p(26f, 21f).y,
                p(32f, 24f).x, p(32f, 24f).y,
                p(32f, 34f).x, p(32f, 34f).y
            )
            lineTo(p(30f, 30f).x, p(30f, 30f).y)
            lineTo(p(20f, 25f).x, p(20f, 25f).y)
            lineTo(p(10f, 30f).x, p(10f, 30f).y)
            close()
        }
        drawPath(path = body, color = color)
    }
}
