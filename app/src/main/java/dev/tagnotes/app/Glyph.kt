package dev.tagnotes.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp

@Composable
fun Glyph(name: String, description: String? = null) {
    val ink = LocalContentColor.current
    Canvas(Modifier.size(24.dp).semantics { description?.let { contentDescription = it } }) {
        val u = size.width / 24f
        fun point(x: Int, y: Int) = Offset(x*u, y*u)
        fun line(x: Int,y: Int,a: Int,b: Int) = drawLine(ink,point(x,y),point(a,b),1.8f*u,StrokeCap.Round)
        val pen = Stroke(width=1.8f*u)
        when (name) {
            "plus" -> { line(5,12,19,12); line(12,5,12,19) }
            "search" -> { drawCircle(ink,6*u,point(10,10),style=pen); line(15,15,21,21) }
            "close" -> { line(6,6,18,18); line(6,18,18,6) }
            "back" -> { line(4,12,20,12); line(4,12,11,5); line(4,12,11,19) }
            "book" -> { drawRoundRect(ink,point(4,3),Size(16*u,18*u),CornerRadius(2*u),style=pen); line(9,4,9,20); line(12,8,16,8) }
            "settings" -> { line(4,7,20,7); line(4,17,20,17); drawCircle(ink,3*u,point(9,7)); drawCircle(ink,3*u,point(16,17)) }
        }
    }
}
