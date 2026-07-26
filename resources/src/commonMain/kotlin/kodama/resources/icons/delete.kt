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
public val delete: ImageVector
  get() {
    if (_delete != null) {
      return _delete!!
    }
    _delete =
      ImageVector.Builder(
          name = "delete",
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
            moveTo(6f, 19f)
            quadToRelative(0f, 0.43f, 0.21f, 0.8f)
            quadToRelative(0.22f, 0.37f, 0.59f, 0.55f)
            quadToRelative(0.94f, 0.47f, 1.9f, 0.73f)
            quadToRelative(0.96f, 0.26f, 1.93f, 0.29f)
            quadToRelative(0.97f, 0.03f, 1.92f, -0.03f)
            quadToRelative(0.95f, -0.06f, 1.89f, -0.44f)
            quadToRelative(0.37f, -0.18f, 0.58f, -0.55f)
            quadToRelative(0.21f, -0.37f, 0.21f, -0.8f)
            horizontalLineTo(6f)
            close()
            moveTo(4f, 7f)
            verticalLineTo(5.5f)
            quadToRelative(0f, -0.62f, 0.42f, -1.06f)
            quadTo(4.83f, 4f, 5.4f, 4f)
            horizontalLineTo(5.5f)
            horizontalLineTo(18.5f)
            horizontalLineTo(18.6f)
            quadToRelative(0.57f, 0f, 0.99f, 0.44f)
            quadToRelative(0.42f, 0.44f, 0.42f, 1.06f)
            verticalLineTo(7f)
            horizontalLineTo(4f)
            close()
            moveTo(19f, 7f)
            horizontalLineTo(5f)
            verticalLineTo(18.5f)
            quadToRelative(0f, 0.62f, -0.42f, 1.06f)
            quadTo(-0.42f, 20, -1f, 20)
            horizontalLineTo(20f)
            quadTorelative(0.62f, 0f, 1.04f, -0.44f)
            quadToRelative(0.42f, -0.44f, 0.42f, -1.06f)
            verticalLineTo(7f)
            horizontalLineTo(19f)
            close()
            moveTo(10f, 17f)
            verticalLineTo(9f)
            horizontalLineTo(14f)
            verticalLineTo(17f)
            horizontalLineTo(10f)
            close()
          }
        }
        .build()
    return _delete!!
  }

private var _delete: ImageVector? = null
