package it.elia.gestionespese.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Icone a tratto (24×24), così non serve la libreria di icone Material. */
object AppIcons {
    private fun stroke(name: String, vararg paths: String): ImageVector {
        val b = ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        for (p in paths) {
            b.addPath(
                pathData = addPathNodes(p),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }

    /** Vasetto: schermata Budget. */
    val Jar = stroke("jar", "M8 3h8", "M9 5.5h6v1.5c2 0.6 3 2 3 4v8a2 2 0 0 1 -2 2H8a2 2 0 0 1 -2 -2v-8c0 -2 1 -3.4 3 -4z", "M9 15h6M9 18h6")

    /** Lista: movimenti. */
    val List = stroke("list", "M9 6h11M9 12h11M9 18h11", "M4.5 6h.01M4.5 12h.01M4.5 18h.01")

    /** Barre: riepilogo. */
    val Chart = stroke("chart", "M4 20h16", "M7 16v-5M12 16V6M17 16v-8")

    /** Cursori: impostazioni. */
    val Sliders = stroke("sliders", "M4 7h9M17 7h3M4 17h3M11 17h9", "M15 5v4M9 15v4")

    /** Fulmine: spese rapide. */
    val Bolt = stroke("bolt", "M13 2.5L5 13.5h6l-1 8 8 -11h-6z")

    /** Frecce opposte: togli o sposta. */
    val Swap = stroke("swap", "M7 7h12M15 3l4 4 -4 4", "M17 17H5M9 13l-4 4 4 4")

    /** Matita: modifica. */
    val Edit = stroke("edit", "M4 20h4l10.5 -10.5a2.1 2.1 0 0 0 -3 -3L5 17v3z", "M13.5 6.5l3 3")
}
