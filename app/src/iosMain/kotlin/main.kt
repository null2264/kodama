import androidx.compose.ui.window.ComposeUIViewController
import io.github.null2264.bonsai.App
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController { App() }
