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
public val draft_orders: ImageVector
  get() {
    if (_draft_orders != null) {
      return _draft_orders!!
    }
    _draft_orders =
      ImageVector.Builder(
          name = "draft_orders",
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
            moveTo(12f, 22f)
            quadTo(9.93f, 22f, 8.1f, 21.21f)
            quadTo(6.28f, 20.43f, 4.93f, 19.08f)
            quadTo(3.58f, 17.73f, 2.79f, 15.9f)
            reflectiveQuadTo(2f, 12f)
            quadTo(2f, 9.92f, 2.79f, 8.1f)
            quadTo(3.58f, 6.27f, 4.93f, 4.93f)
            quadTo(6.28f, 3.57f, 8.1f, 2.79f)
            quadTo(9.93f, 2f, 12f, 2f)
            reflectiveQuadToRelative(3.9f, 0.79f)
            reflectiveQuadToRelative(3.17f, 2.14f)
            quadToRelative(1.35f, 1.35f, 2.14f, 3.17f)
            quadTo(22f, 9.92f, 22f, 12f)
            reflectiveQuadToRelative(-0.79f, 3.9f)
            reflectiveQuadToRelative(-2.14f, 3.17f)
            quadToRelative(-1.35f, 1.35f, -3.17f, 2.14f)
            reflectiveQuadTo(12f, 22f)
            close()
            moveToRelative(0f, -2f)
            quadToRelative(3.35f, 0f, 5.68f, -2.32f)
            reflectiveQuadTo(20f, 12f)
            reflectiveQuadTo(17.68f, 6.32f)
            reflectiveQuadTo(12f, 4f)
            reflectiveQuadTo(6.33f, 6.32f)
            reflectiveQuadTo(4f, 12f)
            reflectiveQuadToRelative(2.33f, 5.68f)
            reflectiveQuadTo(12f, 20f)
            close()
            moveTo(8f, 16f)
            verticalLineTo(12.93f)
            lineToRelative(5.53f, -5.5f)
            quadTo(13.75f, 7.2f, 14.03f, 7.1f)
            reflectiveQuadTo(14.58f, 7f)
            quadToRelative(0.3f, 0f, 0.57f, 0.11f)
            quadToRelative(0.28f, 0.11f, 0.5f, 0.34f)
            lineToRelative(0.92f, 0.93f)
            quadToRelative(0.2f, 0.22f, 0.31f, 0.5f)
            reflectiveQuadTo(17f, 9.42f)
            reflectiveQuadTo(16.9f, 9.99f)
            reflectiveQuadTo(16.58f, 10.5f)
            lineTo(11.08f, 16f)
            horizontalLineTo(8f)
            close()
            moveTo(15.5f, 9.42f)
            lineTo(14.58f, 8.5f)
            lineTo(15.5f, 9.42f)
            close()
            moveToRelative(-6f, 5.08f)
            horizontalLineToRelative(0.95f)
            lineToRelative(3.03f, -3.05f)
            lineTo(13.03f, 10.98f)
            lineTo(12.55f, 10.52f)
            lineTo(9.5f, 13.55f)
            verticalLineTo(14.5f)
            close()
            moveToRelative(3.53f, -3.53f)
            lineTo(12.55f, 10.52f)
            lineToRelative(0.93f, 0.93f)
            lineTo(13.03f, 10.98f)
            close()
          }
        }
        .build()
    return _draft_orders!!
  }

private var _draft_orders: ImageVector? = null
