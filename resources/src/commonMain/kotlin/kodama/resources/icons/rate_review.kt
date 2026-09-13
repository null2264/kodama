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
public val rate_review: ImageVector
  get() {
    if (_rate_review != null) {
      return _rate_review!!
    }
    _rate_review =
      ImageVector.Builder(
          name = "rate_review",
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
            moveTo(6f, 14f)
            horizontalLineTo(9.05f)
            lineToRelative(5f, -5f)
            quadTo(14.28f, 8.77f, 14.39f, 8.49f)
            reflectiveQuadTo(14.5f, 7.93f)
            reflectiveQuadTo(14.38f, 7.39f)
            reflectiveQuadTo(14.05f, 6.9f)
            lineTo(13.15f, 5.95f)
            quadTo(12.93f, 5.72f, 12.65f, 5.61f)
            reflectiveQuadTo(12.08f, 5.5f)
            quadToRelative(-0.28f, 0f, -0.56f, 0.11f)
            quadTo(11.23f, 5.72f, 11f, 5.95f)
            lineToRelative(-5f, 5f)
            verticalLineTo(14f)
            close()
            moveTo(13f, 7.93f)
            lineTo(12.08f, 7f)
            lineTo(13f, 7.93f)
            close()
            moveTo(7.5f, 12.5f)
            verticalLineTo(11.55f)
            lineTo(10.03f, 9.02f)
            lineToRelative(0.5f, 0.45f)
            lineToRelative(0.45f, 0.5f)
            lineTo(8.45f, 12.5f)
            horizontalLineTo(7.5f)
            close()
            moveTo(10.53f, 9.48f)
            lineToRelative(0.45f, 0.5f)
            lineTo(10.03f, 9.02f)
            lineToRelative(0.5f, 0.45f)
            close()
            moveTo(11.18f, 14f)
            horizontalLineTo(18f)
            verticalLineTo(12f)
            horizontalLineTo(13.18f)
            lineToRelative(-2f, 2f)
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
    return _rate_review!!
  }

private var _rate_review: ImageVector? = null
