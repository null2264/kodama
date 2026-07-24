package kodama.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kodama.resources.icons.alternate_email
import kodama.resources.icons.visibility
import kodama.resources.icons.visibility_off

@Composable
fun KodamaTextField(
    value: String,
    label: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    textFieldsColors: TextFieldColors = TextFieldDefaults.colors(),
    isPassword: Boolean = false,
) {
    var isPasswordVisible by remember { mutableStateOf(!isPassword) }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = TextStyle(
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = textFieldsColors.unfocusedTextColor,
            letterSpacing = 0.5.sp
        ),
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        decorationBox = { innerTextField ->
            Column(
                modifier = Modifier
                    .background(textFieldsColors.unfocusedContainerColor, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = label,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textFieldsColors.unfocusedLabelColor,
                        letterSpacing = 0.5.sp
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    icon()

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textFieldsColors.unfocusedLabelColor.copy(alpha = 0.6f),
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }

                        innerTextField()
                    }

                    if (isPassword && value.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isPasswordVisible) visibility_off else visibility,
                                contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                tint = textFieldsColors.unfocusedTextColor,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable { isPasswordVisible = !isPasswordVisible }
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
@Preview
fun KodamaTextFieldPreview() {
    KodamaTextField(
        value = "",
        label = "Password",
        placeholder = "Minimal 8 karakter",
        onValueChange = {},
        icon = {
            Icon(
                imageVector = alternate_email,
                contentDescription = "Email Icon",
                tint = Color(0xFF333333),
                modifier = Modifier.size(24.dp)
            )
        },
        isPassword = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
    )
}
