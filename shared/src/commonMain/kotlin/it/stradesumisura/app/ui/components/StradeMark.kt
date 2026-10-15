package it.stradesumisura.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import it.stradesumisura.app.ui.theme.Mauve

/**
 * The STRADE mark: a diagonal mauve road crossing behind a black band carrying the spaced
 * wordmark, with the "su Misura" caption at the bottom right. Drawn as vector graphics so it
 * stays crisp at any size and adapts to light/dark themes.
 */
@Composable
fun StradeMark(
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
    bandColor: Color = MaterialTheme.colorScheme.onBackground,
    wordColor: Color = MaterialTheme.colorScheme.background,
    diagonalColor: Color = Mauve,
    captionColor: Color = MaterialTheme.colorScheme.onBackground,
    showCaption: Boolean = true,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val heightPx = with(density) { height.toPx() }
    val bandHeight = heightPx * 0.46f
    val bandTop = heightPx * 0.27f
    val wordSizePx = bandHeight * 0.56f

    val wordStyle = TextStyle(
        fontSize = with(density) { wordSizePx.toSp() },
        fontWeight = FontWeight.Black,
        letterSpacing = 0.22.em,
        color = wordColor,
    )
    val word = remember(wordStyle, measurer) { measurer.measure(AnnotatedString("STRADE"), wordStyle) }
    val captionStyle = TextStyle(
        fontSize = with(density) { (heightPx * 0.17f).toSp() },
        fontWeight = FontWeight.Bold,
        color = captionColor,
    )
    val caption = remember(captionStyle, measurer) { measurer.measure(AnnotatedString("su Misura"), captionStyle) }

    val trailing = wordSizePx * 0.22f
    val pad = bandHeight * 0.55f
    val bandWidth = word.size.width - trailing + pad * 2f
    val widthDp = with(density) { bandWidth.toDp() }

    Canvas(modifier.size(width = widthDp, height = height)) {
        val cx = bandWidth * 0.40f
        val cy = bandTop + bandHeight / 2f
        val length = heightPx * 1.0f
        rotate(degrees = -40f, pivot = Offset(cx, cy)) {
            drawRect(
                color = diagonalColor,
                topLeft = Offset(cx - length / 2f, cy - bandHeight / 2f),
                size = Size(length, bandHeight),
            )
        }
        drawRect(color = bandColor, topLeft = Offset(0f, bandTop), size = Size(bandWidth, bandHeight))
        drawText(word, topLeft = Offset(pad, bandTop + (bandHeight - word.size.height) / 2f))
        if (showCaption) {
            drawText(caption, topLeft = Offset(bandWidth - caption.size.width, bandTop + bandHeight + heightPx * 0.04f))
        }
    }
}

/** Compact text-only wordmark for app bars. */
@Composable
fun StradeWordmark(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            text = "STRADE",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 0.18.em),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = " su Misura",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
