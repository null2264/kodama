package kodama.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
public val reviews: ImageVector
  get() {
    if (_reviews != null) {
      return _reviews!!
    }
    _reviews =
      ImageVector.Builder(
          name = "reviews",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.NonZero,
          ) {
            moveTo(9.08f, 14.25f)
            lineTo(12f, 12.48f)
            lineToRelative(2.93f, 1.78f)
            lineTo(14.15f, 10.93f)
            lineToRelative(2.6f, -2.25f)
            lineTo(13.33f, 8.4f)
            lineTo(12f, 5.25f)
            lineTo(10.68f, 8.4f)
            lineTo(7.25f, 8.67f)
            lineToRelative(2.6f, 2.25f)
            lineTo(9.08f, 14.25f)
            close()
            moveTo(2f, 22f)
            verticalLineTo(4f)
            quadTo(2f, 3.17f, 2.59f, 2.59f)
            reflectiveQuadTo(4f, 2f)
            horizontalLineTo(20f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(22f, 4f)
            verticalLineTo(16f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(20f, 18f)
            horizontalLineTo(6f)
            lineTo(2f, 22f)
            close()
            moveTo(5.15f, 16f)
            horizontalLineTo(20f)
            verticalLineTo(4f)
            horizontalLineTo(4f)
            verticalLineTo(17.13f)
            lineTo(5.15f, 16f)
            close()
            moveTo(4f, 16f)
            verticalLineTo(4f)
            verticalLineTo(16f)
            close()
          }
        }
        .build()
    return _reviews!!
  }

private var _reviews: ImageVector? = null
