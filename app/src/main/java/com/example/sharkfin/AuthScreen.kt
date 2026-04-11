package com.example.sharkfin

// ─── Imports ───────────────────────────────────────────────────────────────
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// ─── Auth Mode ─────────────────────────────────────────────────────────────
enum class AuthMode { LOGIN, SIGNUP }

val accountTypes = listOf("Individual", "Joint", "Family", "Business")

@Composable
fun AuthScreen(
    onLogin: (String, String) -> Unit,
    onSignup: (String, String, String, String) -> Unit
) {
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedAccountType by remember { mutableStateOf("Individual") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SharkBg, SharkSurface),
                    startY = 0f,
                    endY = 1000f
                )
            )
    ) {
        // ── TOP BRANDING SECTION ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .padding(top = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = R.drawable.sharkfinlogo,
                contentDescription = "SharkFin Logo",
                modifier = Modifier.size(120.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row {
                Text(
                    text = "Shark",
                    color = SharkLabel,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light
                )
                Text(
                    text = "Fin",
                    color = SharkGold,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "FINANCIAL GROWTH",
                color = SharkSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }

        val imeInsets = WindowInsets.ime
        val keyboardHeight = imeInsets.getBottom(LocalDensity.current)
        val translationY by animateFloatAsState(
            targetValue = if (keyboardHeight > 0) -keyboardHeight * 0.4f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            ), label = "keyboard_slide"
        )

        // ── BOTTOM CARD ───────────────────────────────────────────────────
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer { this.translationY = translationY }
                .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)),
            color = SharkSurface.copy(alpha = 0.95f),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp, vertical = 40.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AuthSlider(
                    selectedMode = authMode,
                    onModeChange = {
                        authMode = it
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (authMode == AuthMode.SIGNUP) {
                    AuthInputField(
                        value = name,
                        onValueChange = { name = it },
                        hint = "Full Name",
                        icon = Icons.Default.Person
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, SharkCardBorder, RoundedCornerShape(20.dp))
                            .background(SharkBg.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .clickable { dropdownExpanded = true }
                            .padding(horizontal = 16.dp, vertical = 18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$selectedAccountType Account",
                                color = SharkLabel,
                                fontSize = 16.sp
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Select Account Type",
                                tint = SharkGold
                            )
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.background(SharkSurface).border(0.5.dp, SharkCardBorder)
                        ) {
                            accountTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "$type Account",
                                            color = SharkLabel
                                        )
                                    },
                                    onClick = {
                                        selectedAccountType = type
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                AuthInputField(
                    value = email,
                    onValueChange = { email = it },
                    hint = "Email Address",
                    icon = Icons.Default.Email
                )

                Spacer(modifier = Modifier.height(16.dp))

                AuthInputField(
                    value = password,
                    onValueChange = { password = it },
                    hint = "Password",
                    icon = Icons.Default.Lock,
                    isPassword = true
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (authMode == AuthMode.LOGIN) {
                            onLogin(email, password)
                        } else {
                            onSignup(name, email, password, selectedAccountType.uppercase())
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .graphicsLayer {
                            // Simple subtle scale effect on interaction could be added here
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SharkGold,
                        contentColor = SharkBg
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 0.dp)
                ) {
                    Text(
                        text = if (authMode == AuthMode.LOGIN) "Access Account" else "Create Portfolio",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (authMode == AuthMode.LOGIN)
                        "Secure biometric login enabled"
                    else
                        "By signing up, you agree to our Terms",
                    color = SharkSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun AuthSlider(
    selectedMode: AuthMode,
    onModeChange: (AuthMode) -> Unit
) {
    val transition = updateTransition(targetState = selectedMode, label = "AuthSlider")
    val offsetX by transition.animateDp(label = "pillOffset") { mode ->
        if (mode == AuthMode.LOGIN) 0.dp else 140.dp
    }

    Box(
        modifier = Modifier
            .width(280.dp)
            .height(48.dp)
            .background(SharkBg.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .border(0.5.dp, SharkCardBorder, RoundedCornerShape(24.dp))
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(x = offsetX)
                .width(136.dp)
                .fillMaxHeight()
                .background(SharkGold, RoundedCornerShape(20.dp))
        )

        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onModeChange(AuthMode.LOGIN) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Log In",
                    color = if (selectedMode == AuthMode.LOGIN) SharkBg else SharkSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onModeChange(AuthMode.SIGNUP) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Sign Up",
                    color = if (selectedMode == AuthMode.SIGNUP) SharkBg else SharkSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthInputField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    icon: ImageVector,
    isPassword: Boolean = false
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, SharkCardBorder, RoundedCornerShape(20.dp)),
        placeholder = {
            Text(hint, color = SharkTertiary)
        },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = SharkGold.copy(alpha = 0.7f)
            )
        },
        visualTransformation = if (isPassword)
            PasswordVisualTransformation()
        else
            androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = if (isPassword)
            KeyboardOptions(keyboardType = KeyboardType.Password)
        else
            KeyboardOptions.Default,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = SharkBg.copy(alpha = 0.5f),
            unfocusedContainerColor = SharkBg.copy(alpha = 0.3f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedTextColor = SharkLabel,
            unfocusedTextColor = SharkLabel
        ),
        shape = RoundedCornerShape(20.dp)
    )
}
