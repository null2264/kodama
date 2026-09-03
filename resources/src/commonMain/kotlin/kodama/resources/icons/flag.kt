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
public val flag: ImageVector
  get() {
    if (_flag_2 != null) {
      return _flag_2!!
    }
    _flag_2 =
      ImageVector.Builder(
          name = "flag_2",
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
            moveTo(5f, 22f)
            verticalLineTo(3f)
            horizontalLineTo(21f)
            lineTo(19f, 8f)
            lineToRelative(2f, 5f)
            horizontalLineTo(7f)
            verticalLineToRelative(9f)
            horizontalLineTo(5f)
            close()
            moveTo(7f, 11f)
            horizontalLineTo(18.05f)
            lineTo(16.85f, 8f)
            lineToRelative(1.2f, -3f)
            horizontalLineTo(7f)
            verticalLineToRelative(6f)
            close()
            moveToRelative(0f, 0f)
            verticalLineTo(5f)
            verticalLineTo(8f)
            verticalLineToRelative(3f)
            close()
          }
        }
        .build()
    return _flag_2!!
  }

private var _flag_2: ImageVector? = null
