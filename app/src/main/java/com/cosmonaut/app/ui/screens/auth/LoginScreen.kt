package com.cosmonaut.app.ui.screens.auth

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.cosmonaut.app.R
import com.cosmonaut.app.ui.components.GlassCard
import com.cosmonaut.app.ui.screens.auth.components.ForgotPasswordForm
import com.cosmonaut.app.ui.screens.auth.components.SignInForm
import com.cosmonaut.app.ui.screens.auth.components.SignUpForm
import com.cosmonaut.app.ui.screens.auth.components.VerifyForm
import com.cosmonaut.app.ui.theme.CosmoTheme
import com.cosmonaut.app.ui.theme.OrbitronFontFamily

private const val SLIDE_OFFSET_DIVISOR = 3
private const val GLOW_RADIUS_FRACTION = 0.6f
private const val GLOW_ALPHA = 0.07f

private val taglines = listOf(
    "Don't forget your spacesuit!",
    "Not all who wander are lost",
    "The universe is waiting for you",
    "A good story is like a good meal",
    "Powered by Claude",
    "Try narration out!",
)

@Composable
fun LoginScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val tagline = remember { taglines.random() }
    val primaryColor = CosmoTheme.colors.primary

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.NavigateToDashboard -> onAuthSuccess()
                is LoginEvent.ShowMessage -> snackbarHostState.showSnackbar(
                    message = event.message,
                    duration = SnackbarDuration.Short,
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CosmoTheme.colors.background)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = GLOW_ALPHA),
                            Color.Transparent,
                        ),
                        center = Offset(size.width / 2f, size.height * 0.18f),
                        radius = size.width * GLOW_RADIUS_FRACTION,
                    ),
                    radius = size.width * GLOW_RADIUS_FRACTION,
                    center = Offset(size.width / 2f, size.height * 0.18f),
                )
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Logo + Brand
            BrandHeader(tagline = tagline)

            Spacer(modifier = Modifier.height(20.dp))

            // Subtitle
            Text(
                text = viewSubtitle(state.view),
                style = MaterialTheme.typography.bodyMedium,
                color = CosmoTheme.colors.mutedForeground,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Messages
            state.errorMessage?.let { error ->
                MessageBanner(
                    text = error,
                    color = CosmoTheme.colors.destructive,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            state.successMessage?.let { message ->
                MessageBanner(
                    text = message,
                    color = CosmoTheme.colors.primary,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (state.isSuspended) {
                SuspendedNotice()
            } else {
                GlassCard {
                    AnimatedContent(
                        targetState = state.view,
                        transitionSpec = {
                            (slideInHorizontally { it / SLIDE_OFFSET_DIVISOR } + fadeIn())
                                .togetherWith(slideOutHorizontally { -it / SLIDE_OFFSET_DIVISOR } + fadeOut())
                        },
                        label = "loginView",
                    ) { view ->
                        Column(modifier = Modifier.padding(24.dp)) {
                            when (view) {
                                LoginView.SIGN_IN -> SignInForm(
                                    email = state.email,
                                    password = state.password,
                                    showPassword = state.showPassword,
                                    isSubmitting = state.isSubmitting,
                                    onEmailChange = viewModel::updateEmail,
                                    onPasswordChange = viewModel::updatePassword,
                                    onTogglePassword = viewModel::togglePasswordVisibility,
                                    onSignIn = viewModel::signIn,
                                    onGoogleSignIn = {
                                        (context as? Activity)?.let { viewModel.signInWithGoogle(it) }
                                    },
                                    onSwitchToSignUp = viewModel::switchToSignUp,
                                    onForgotPassword = viewModel::switchToForgotPassword,
                                )
                                LoginView.SIGN_UP -> SignUpForm(
                                    email = state.email,
                                    password = state.password,
                                    confirmPassword = state.confirmPassword,
                                    showPassword = state.showPassword,
                                    isSubmitting = state.isSubmitting,
                                    onEmailChange = viewModel::updateEmail,
                                    onPasswordChange = viewModel::updatePassword,
                                    onConfirmPasswordChange = viewModel::updateConfirmPassword,
                                    onTogglePassword = viewModel::togglePasswordVisibility,
                                    onSignUp = viewModel::signUp,
                                    onGoogleSignIn = {
                                        (context as? Activity)?.let { viewModel.signInWithGoogle(it) }
                                    },
                                    onSwitchToSignIn = viewModel::switchToSignIn,
                                )
                                LoginView.VERIFY -> VerifyForm(
                                    email = state.email,
                                    code = state.verificationCode,
                                    isSubmitting = state.isSubmitting,
                                    onCodeChange = viewModel::updateVerificationCode,
                                    onVerify = viewModel::verify,
                                    onResendCode = viewModel::resendCode,
                                    onBackToSignIn = viewModel::switchToSignIn,
                                )
                                LoginView.FORGOT -> ForgotPasswordForm(
                                    isResetMode = false,
                                    email = state.email,
                                    code = state.resetCode,
                                    newPassword = state.newPassword,
                                    confirmNewPassword = state.confirmNewPassword,
                                    showPassword = state.showPassword,
                                    isSubmitting = state.isSubmitting,
                                    onEmailChange = viewModel::updateEmail,
                                    onCodeChange = viewModel::updateResetCode,
                                    onNewPasswordChange = viewModel::updateNewPassword,
                                    onConfirmNewPasswordChange = viewModel::updateConfirmNewPassword,
                                    onTogglePassword = viewModel::togglePasswordVisibility,
                                    onSendCode = viewModel::sendResetCode,
                                    onResetPassword = viewModel::resetPassword,
                                    onBackToSignIn = viewModel::switchToSignIn,
                                )
                                LoginView.RESET -> ForgotPasswordForm(
                                    isResetMode = true,
                                    email = state.email,
                                    code = state.resetCode,
                                    newPassword = state.newPassword,
                                    confirmNewPassword = state.confirmNewPassword,
                                    showPassword = state.showPassword,
                                    isSubmitting = state.isSubmitting,
                                    onEmailChange = viewModel::updateEmail,
                                    onCodeChange = viewModel::updateResetCode,
                                    onNewPasswordChange = viewModel::updateNewPassword,
                                    onConfirmNewPasswordChange = viewModel::updateConfirmNewPassword,
                                    onTogglePassword = viewModel::togglePasswordVisibility,
                                    onSendCode = viewModel::sendResetCode,
                                    onResetPassword = viewModel::resetPassword,
                                    onBackToSignIn = viewModel::switchToSignIn,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            SnackbarHost(hostState = snackbarHostState)
        }
    }
}

@Composable
private fun BrandHeader(tagline: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CosmoTheme.colors.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = "Cosmonaut logo",
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = "Cosmonaut",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = OrbitronFontFamily,
                ),
                color = CosmoTheme.colors.foreground,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = tagline.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = OrbitronFontFamily,
                letterSpacing = 2.sp,
            ),
            color = CosmoTheme.colors.primary.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
    }
}


@Composable
private fun MessageBanner(text: String, color: Color, modifier: Modifier = Modifier,) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun SuspendedNotice(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CosmoTheme.colors.destructive.copy(alpha = 0.1f))
            .border(
                width = 1.dp,
                color = CosmoTheme.colors.destructive.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Account Suspended",
                style = MaterialTheme.typography.titleMedium,
                color = CosmoTheme.colors.destructive,
            )
            Text(
                text = "Your account has been suspended for violating our Terms of Service. " +
                    "If you believe this is a mistake, please contact support@cosmonaut-ai.com.",
                style = MaterialTheme.typography.bodyMedium,
                color = CosmoTheme.colors.foreground,
            )
        }
    }
}

private fun viewSubtitle(view: LoginView): String = when (view) {
    LoginView.SIGN_IN -> "Sign in to continue your adventures"
    LoginView.SIGN_UP -> "Create your account"
    LoginView.VERIFY -> "Verify your email"
    LoginView.FORGOT -> "Reset your password"
    LoginView.RESET -> "Enter your new password"
}
