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
public val alternate_email: ImageVector
  get() {
    if (_alternate_email != null) {
      return _alternate_email!!
    }
    _alternate_email =
      ImageVector.Builder(
          name = "alternate_email",
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
            pathFillType = PathFillType.Companion.NonZero,
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
            verticalLineToRelative(1.45f)
            quadToRelative(0f, 1.47f, -1.01f, 2.51f)
            reflectiveQuadTo(18.5f, 17f)
            quadToRelative(-0.88f, 0f, -1.65f, -0.38f)
            reflectiveQuadToRelative(-1.3f, -1.08f)
            quadToRelative(-0.72f, 0.72f, -1.64f, 1.09f)
            reflectiveQuadTo(12f, 17f)
            quadTo(9.93f, 17f, 8.46f, 15.54f)
            reflectiveQuadTo(7f, 12f)
            quadTo(7f, 9.92f, 8.46f, 8.46f)
            reflectiveQuadTo(12f, 7f)
            reflectiveQuadToRelative(3.54f, 1.46f)
            reflectiveQuadTo(17f, 12f)
            verticalLineToRelative(1.45f)
            quadToRelative(0f, 0.65f, 0.43f, 1.1f)
            reflectiveQuadTo(18.5f, 15f)
            reflectiveQuadToRelative(1.08f, -0.45f)
            reflectiveQuadTo(20f, 13.45f)
            verticalLineTo(12f)
            quadTo(20f, 8.65f, 17.68f, 6.32f)
            reflectiveQuadTo(12f, 4f)
            reflectiveQuadTo(6.33f, 6.32f)
            reflectiveQuadTo(4f, 12f)
            reflectiveQuadToRelative(2.33f, 5.68f)
            reflectiveQuadTo(12f, 20f)
            horizontalLineToRelative(5f)
            verticalLineToRelative(2f)
            horizontalLineTo(12f)
            close()
            moveToRelative(2.13f, -7.88f)
            quadTo(15f, 13.25f, 15f, 12f)
            reflectiveQuadTo(14.13f, 9.88f)
            reflectiveQuadTo(12f, 9f)
            reflectiveQuadTo(9.88f, 9.88f)
            reflectiveQuadTo(9f, 12f)
            reflectiveQuadToRelative(0.88f, 2.13f)
            reflectiveQuadTo(12f, 15f)
            reflectiveQuadToRelative(2.13f, -0.88f)
            close()
          }
        }
        .build()
    return _alternate_email!!
  }

private var _alternate_email: ImageVector? = null
