package com.example.eventia.ui.screens.session

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.eventia.R
import com.example.eventia.repositories.AccountField

private val FestivalForest = com.example.eventia.ui.theme.Ink
private val FestivalShade = Color(0xFF102C1A)
private val FestivalMint = com.example.eventia.ui.theme.PaleSage
private val FestivalError = Color(0xFFFFDAD7)
private val FestivalButtonShape = RoundedCornerShape(9.dp)

@Composable
internal fun FestivalSessionForm(
    form: SessionUiState.Form,
    name: String,
    email: String,
    password: String,
    confirmation: String,
    confirmationError: Int?,
    scrollState: ScrollState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmationChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSwitch: () -> Unit,
    onGuest: () -> Unit
) {
    val enabled = !form.submitting
    val registering = form.registering
    val artwork = painterResource(R.drawable.eventia_login_festival)

    BoxWithConstraints(Modifier.fillMaxSize().background(FestivalShade)) {
        // Keep the illustration and its anchors at their original size when the IME opens.
        val sceneHeight = maxHeight.coerceAtLeast(650.dp)
        val viewportHeight = maxHeight
        val formWidth = (maxWidth * 0.80f).coerceAtMost(410.dp)

        Box(Modifier.fillMaxSize().imePadding().verticalScroll(scrollState)) {
            Box(Modifier.fillMaxWidth().heightIn(min = viewportHeight)) {
                Box(
                    Modifier.matchParentSize().drawWithCache {
                        val frame = Size(size.width, sceneHeight.roundToPx().toFloat())
                        val crop = ContentScale.Crop.computeScaleFactor(artwork.intrinsicSize, frame)
                        val artSize = Size(
                            artwork.intrinsicSize.width * crop.scaleX,
                            artwork.intrinsicSize.height * crop.scaleY
                        )
                        val left = (frame.width - artSize.width) / 2
                        val top = (frame.height - artSize.height) / 2
                        val gradient = Brush.verticalGradient(
                            0.62f to Color.Transparent,
                            0.68f to FestivalShade.copy(alpha = 0.61f),
                            0.88f to FestivalShade.copy(alpha = 0.68f),
                            1f to FestivalShade.copy(alpha = 0.38f),
                            endY = frame.height
                        )
                        onDrawBehind {
                            clipRect(bottom = frame.height) {
                                translate(left, top) { with(artwork) { draw(artSize) } }
                            }
                            if (size.height > frame.height) {
                                // Extend the lower lawn without rescaling or shifting the festival.
                                clipRect(top = frame.height, bottom = minOf(size.height, frame.height * 2)) {
                                    scale(1f, -1f, pivot = Offset(0f, frame.height)) {
                                        translate(left, top) { with(artwork) { draw(artSize) } }
                                    }
                                }
                            }
                            drawRect(gradient)
                        }
                    }
                )

                Column(
                    Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(top = sceneHeight * 0.145f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painterResource(R.drawable.eventia_login_wordmark),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(0.55f)
                            .semantics { heading() }
                    )
                    Text(
                        stringResource(R.string.auth_intro),
                        Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = FestivalForest,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }

                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .padding(top = viewportHeight * 0.22f)
                        .then(
                            if (registering) {
                                Modifier.drawWithCache {
                                    val shade = Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        (24.dp.toPx() / size.height).coerceAtMost(0.5f) to
                                            FestivalShade.copy(alpha = 0.68f),
                                        0.54f to FestivalShade.copy(alpha = 0.68f),
                                        1f to Color.Transparent
                                    )
                                    onDrawBehind { drawRect(shade) }
                                }
                            } else Modifier
                        )
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(top = if (registering) 24.dp else 0.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        Modifier.widthIn(max = formWidth).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (registering) {
                            FestivalField(
                                name, R.string.auth_name, R.drawable.ic_profile,
                                form.errors[AccountField.Name], enabled, KeyboardType.Text, onNameChange,
                                imeAction = ImeAction.Next
                            )
                        }
                        FestivalField(
                            email, R.string.auth_email, R.drawable.ic_session_mail,
                            form.errors[AccountField.Email], enabled, KeyboardType.Email, onEmailChange,
                            imeAction = ImeAction.Next
                        )
                        FestivalField(
                            password, R.string.auth_password, R.drawable.ic_session_lock,
                            form.errors[AccountField.Password], enabled, KeyboardType.Password, onPasswordChange,
                            modifier = Modifier.padding(top = 4.dp),
                            imeAction = if (registering) ImeAction.Next else ImeAction.Done,
                            hint = R.string.auth_password_hint.takeIf { registering }
                        )
                        if (registering) {
                            FestivalField(
                                confirmation, R.string.auth_confirm_password, R.drawable.ic_session_lock,
                                confirmationError, enabled, KeyboardType.Password, onConfirmationChange
                            )
                        }
                        form.error?.let { message ->
                            Text(
                                stringResource(message),
                                Modifier.fillMaxWidth().padding(top = 8.dp),
                                color = FestivalError,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        FestivalAction(
                            label = when {
                                form.submitting -> R.string.auth_submitting
                                registering -> R.string.auth_register
                                else -> R.string.auth_sign_in
                            },
                            enabled = enabled,
                            onClick = onSubmit,
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            loading = form.submitting
                        )
                        TextButton(
                            onClick = onSwitch,
                            enabled = enabled,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) {
                            Text(stringResource(if (registering) R.string.auth_sign_in else R.string.auth_register))
                        }
                        TextButton(
                            onClick = onGuest,
                            enabled = enabled,
                            colors = ButtonDefaults.textButtonColors(contentColor = FestivalMint)
                        ) {
                            Text(stringResource(R.string.auth_continue_guest), textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FestivalAction(
    label: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    loading: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = FestivalButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primaryContainer,
            contentColor = colors.onPrimaryContainer,
            disabledContainerColor = colors.primaryContainer,
            disabledContentColor = colors.onPrimaryContainer
        ),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                Modifier.padding(end = 4.dp).size(14.dp),
                color = colors.onPrimaryContainer,
                strokeWidth = 2.dp
            )
        }
        Text(stringResource(label), textAlign = TextAlign.Center)
    }
}

@Composable
private fun FestivalField(
    value: String,
    label: Int,
    icon: Int,
    fieldError: Int?,
    enabled: Boolean,
    keyboardType: KeyboardType,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Done,
    hint: Int? = null
) {
    val labelText = stringResource(label)
    val errorText = fieldError?.let { stringResource(it) }
    val isPassword = keyboardType == KeyboardType.Password
    val isConfirmation = label == R.string.auth_confirm_password
    val supportingLabel = fieldError ?: hint
    var passwordVisible by remember { mutableStateOf(false) }

    TextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth().semantics {
            contentDescription = labelText
            errorText?.let { error(it) }
        },
        enabled = enabled,
        singleLine = true,
        label = {
            Text(
                labelText,
                style = if (isConfirmation) MaterialTheme.typography.bodyMedium else LocalTextStyle.current
            )
        },
        leadingIcon = { Icon(painterResource(icon), null, Modifier.size(20.dp)) },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = enabled) {
                    Icon(
                        painterResource(
                            if (passwordVisible) R.drawable.ic_session_eye_off else R.drawable.ic_session_eye
                        ),
                        stringResource(
                            when {
                                isConfirmation && passwordVisible -> R.string.auth_hide_confirmation
                                isConfirmation -> R.string.auth_show_confirmation
                                passwordVisible -> R.string.auth_hide_password
                                else -> R.string.auth_show_password
                            }
                        ),
                        Modifier.size(18.dp)
                    )
                }
            }
        } else null,
        supportingText = supportingLabel?.let { { Text(stringResource(it)) } },
        isError = fieldError != null,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        visualTransformation = if (isPassword && !passwordVisible) {
            PasswordVisualTransformation()
        } else VisualTransformation.None,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            disabledTextColor = Color.White.copy(alpha = 0.65f),
            errorTextColor = Color.White,
            cursorColor = FestivalMint,
            errorCursorColor = FestivalError,
            focusedLabelColor = FestivalMint,
            unfocusedLabelColor = Color.White,
            disabledLabelColor = Color.White.copy(alpha = 0.65f),
            errorLabelColor = FestivalError,
            focusedLeadingIconColor = Color.White,
            unfocusedLeadingIconColor = Color.White,
            disabledLeadingIconColor = Color.White.copy(alpha = 0.65f),
            errorLeadingIconColor = Color.White,
            focusedTrailingIconColor = Color.White,
            unfocusedTrailingIconColor = Color.White,
            disabledTrailingIconColor = Color.White.copy(alpha = 0.65f),
            errorTrailingIconColor = Color.White,
            focusedIndicatorColor = FestivalMint,
            unfocusedIndicatorColor = Color.White,
            disabledIndicatorColor = Color.White.copy(alpha = 0.65f),
            errorIndicatorColor = FestivalError,
            focusedSupportingTextColor = Color.White,
            unfocusedSupportingTextColor = Color.White,
            errorSupportingTextColor = FestivalError
        )
    )
}
