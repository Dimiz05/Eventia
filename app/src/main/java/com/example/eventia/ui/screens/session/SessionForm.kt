package com.example.eventia.ui.screens.session

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.eventia.R
import com.example.eventia.repositories.AccountField
import com.example.eventia.ui.theme.EventiaTheme

private val SessionLightColors = lightColorScheme(
    primary = Color(0xFF007A85),
    onPrimary = Color.White,
    surface = Color.White,
    onSurface = Color(0xFF192C33),
    onSurfaceVariant = Color(0xFF4E6065),
    surfaceContainerLow = Color(0xFFF9F9FA),
    surfaceContainer = Color(0xFFF0F4F6),
    surfaceContainerHigh = Color(0xFFE3F0F5),
    outline = Color(0xFFA8C2C7),
    outlineVariant = Color(0xFFE2E8F0)
)

private val SessionDarkColors = darkColorScheme(
    primary = Color(0xFF80D5DD),
    onPrimary = Color(0xFF00363B),
    surface = Color(0xFF111B1E),
    onSurface = Color(0xFFE3F0F3),
    onSurfaceVariant = Color(0xFFBACBD0),
    surfaceContainerLow = Color(0xFF172529),
    surfaceContainer = Color(0xFF203237),
    surfaceContainerHigh = Color(0xFF253C42),
    outline = Color(0xFF748B91),
    outlineVariant = Color(0xFF3B5156)
)

private val SessionPanelShape = RoundedCornerShape(24.dp)
private val SessionControlShape = RoundedCornerShape(16.dp)

@Composable
fun SessionForm(
    form: SessionUiState.Form,
    onSubmit: (String, String, String) -> Unit,
    onSwitch: () -> Unit,
    onEdit: (AccountField) -> Unit,
    onGuest: () -> Unit
) {
    var name by rememberSaveable(form.registering) { mutableStateOf("") }
    var email by rememberSaveable(form.registering) { mutableStateOf("") }
    var password by remember(form.registering) { mutableStateOf("") }
    var confirmation by remember(form.registering) { mutableStateOf("") }
    var confirmationError by remember(form.registering) { mutableStateOf<Int?>(null) }
    val scrollState = key(form.registering) { rememberScrollState() }
    val enabled = !form.submitting
    val action = if (form.registering) R.string.auth_register else R.string.auth_sign_in
    val focusManager = LocalFocusManager.current
    val switchMode = {
        focusManager.clearFocus()
        onSwitch()
    }

    BackHandler(enabled = form.registering && enabled, onBack = switchMode)
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) SessionDarkColors else SessionLightColors) {
        Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
                .windowInsetsPadding(WindowInsets.safeDrawing).imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                Modifier.widthIn(max = 460.dp).fillMaxWidth().verticalScroll(scrollState)
                    .padding(start = 16.dp, end = 16.dp, top = if (form.registering) 44.dp else 32.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (form.registering) {
                    RegistrationHeader(enabled, switchMode)
                } else {
                    SignInHeader()
                    SessionModeSelector(enabled, switchMode)
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SessionPanelShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 1.dp
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (form.registering) {
                            SessionField(
                                name, R.string.auth_name, R.string.auth_name_example, R.drawable.ic_profile,
                                form.errors[AccountField.Name], enabled
                            ) {
                                name = it
                                onEdit(AccountField.Name)
                            }
                        }
                        SessionField(
                            email, R.string.auth_email, R.string.auth_email_example, R.drawable.ic_session_mail,
                            form.errors[AccountField.Email], enabled, KeyboardType.Email
                        ) {
                            email = it
                            onEdit(AccountField.Email)
                        }
                        key(form.registering) {
                            SessionField(
                                password, R.string.auth_password, R.string.auth_password_placeholder,
                                R.drawable.ic_session_lock, form.errors[AccountField.Password],
                                enabled, KeyboardType.Password, hint = R.string.auth_password_hint
                            ) {
                                password = it
                                confirmationError = null
                                onEdit(AccountField.Password)
                            }
                        }
                        if (form.registering) {
                            SessionField(
                                confirmation, R.string.auth_confirm_password, R.string.auth_password_placeholder,
                                R.drawable.ic_session_lock, confirmationError, enabled, KeyboardType.Password
                            ) {
                                confirmation = it
                                confirmationError = null
                            }
                        }
                        form.error?.let { message ->
                            Surface(
                                shape = SessionControlShape,
                                color = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ) {
                                Text(stringResource(message), Modifier.fillMaxWidth().padding(12.dp))
                            }
                        }
                        Button(
                            onClick = {
                                if (form.registering && password != confirmation) {
                                    confirmationError = R.string.auth_password_mismatch
                                } else {
                                    onSubmit(name, email, password)
                                }
                            },
                            enabled = enabled,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            shape = SessionControlShape
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(if (form.submitting) R.string.auth_submitting else action),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (form.submitting) {
                                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(
                                        painterResource(R.drawable.ic_back), null,
                                        Modifier.size(18.dp).rotate(180f)
                                    )
                                }
                            }
                        }
                    }
                }

                if (form.registering) {
                    TextButton(
                        onClick = switchMode,
                        enabled = enabled,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        val prompt = stringResource(R.string.auth_have_account)
                        val signIn = stringResource(R.string.auth_sign_in)
                        val promptColor = MaterialTheme.colorScheme.onSurfaceVariant
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(color = promptColor, fontWeight = FontWeight.Normal)) { append(prompt) }
                                append(" ")
                                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(signIn) }
                            },
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onGuest,
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = SessionControlShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Icon(painterResource(R.drawable.ic_explore), null, Modifier.size(18.dp))
                        Text(stringResource(R.string.auth_continue_guest), Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SignInHeader() {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Image(
            painterResource(R.drawable.eventia_logo), null,
            Modifier.size(120.dp).clip(RoundedCornerShape(20.dp))
        )
        Text(
            stringResource(R.string.app_name),
            Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(R.string.auth_intro),
            Modifier.widthIn(max = 280.dp),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SessionModeSelector(enabled: Boolean, onRegister: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(CircleShape).background(colors.surfaceContainerHigh)
            .selectableGroup().padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            Modifier.weight(1f).clip(CircleShape).background(colors.primary)
                .selectable(selected = true, enabled = enabled, role = Role.Tab, onClick = {})
                .heightIn(min = 44.dp).padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painterResource(R.drawable.ic_session_check), null, Modifier.size(16.dp), tint = colors.onPrimary)
            Text(
                stringResource(R.string.auth_sign_in), color = colors.onPrimary,
                style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center
            )
        }
        Box(
            Modifier.weight(1f).clip(CircleShape)
                .selectable(selected = false, enabled = enabled, role = Role.Tab, onClick = onRegister)
                .heightIn(min = 44.dp).padding(horizontal = 8.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(R.string.auth_register), color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RegistrationHeader(enabled: Boolean, onBack: () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        IconButton(
            onClick = onBack,
            enabled = enabled,
            modifier = Modifier.align(Alignment.CenterStart).clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Icon(painterResource(R.drawable.ic_back), stringResource(R.string.navigate_back), Modifier.size(20.dp))
        }
        Text(
            stringResource(R.string.auth_register),
            Modifier.padding(horizontal = 52.dp).semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SessionField(
    value: String,
    label: Int,
    placeholder: Int,
    icon: Int,
    error: Int?,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    hint: Int? = null,
    onChange: (String) -> Unit
) {
    val labelText = stringResource(label)
    val colors = MaterialTheme.colorScheme
    val passwordField = keyboardType == KeyboardType.Password
    val confirmationField = label == R.string.auth_confirm_password
    val errorMessage = error?.let { stringResource(it) }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            labelText, Modifier.clearAndSetSemantics {},
            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold
        )
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = labelText
                errorMessage?.let { this.error(it) }
            },
            enabled = enabled,
            singleLine = true,
            shape = SessionControlShape,
            placeholder = {
                Text(stringResource(placeholder), maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            leadingIcon = { Icon(painterResource(icon), null, Modifier.size(20.dp), tint = colors.primary) },
            trailingIcon = if (passwordField) {
                {
                    IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = enabled) {
                        val visibilityLabel = when {
                            confirmationField && passwordVisible -> R.string.auth_hide_confirmation
                            confirmationField -> R.string.auth_show_confirmation
                            passwordVisible -> R.string.auth_hide_password
                            else -> R.string.auth_show_password
                        }
                        Icon(
                            painterResource(if (passwordVisible) R.drawable.ic_session_eye_off else R.drawable.ic_session_eye),
                            stringResource(visibilityLabel),
                            Modifier.size(20.dp)
                        )
                    }
                }
            } else null,
            isError = error != null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (passwordField && !passwordVisible) {
                PasswordVisualTransformation()
            } else VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surfaceContainerLow,
                disabledContainerColor = colors.surfaceContainerLow,
                errorContainerColor = colors.surfaceContainerLow,
                unfocusedBorderColor = colors.outline,
                focusedPlaceholderColor = colors.onSurfaceVariant,
                unfocusedPlaceholderColor = colors.onSurfaceVariant
            )
        )
        val support = error ?: hint
        if (support != null) {
            Text(
                stringResource(support),
                Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = if (error != null) colors.error else colors.onSurfaceVariant
            )
        }
    }
}

@Preview(name = "Sign in", showBackground = true, widthDp = 393, heightDp = 800)
@Preview(name = "Sign in, dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SignInPreview() {
    EventiaTheme { SessionForm(SessionUiState.Form(), { _, _, _ -> }, {}, {}, {}) }
}

@Preview(name = "Create account", showBackground = true, widthDp = 393, heightDp = 800)
@Composable
private fun RegistrationPreview() {
    EventiaTheme { SessionForm(SessionUiState.Form(registering = true), { _, _, _ -> }, {}, {}, {}) }
}
