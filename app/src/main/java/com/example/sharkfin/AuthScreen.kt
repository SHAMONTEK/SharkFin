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
// Two states: are we logging in or signing up?
enum class AuthMode { LOGIN, SIGNUP }

// ─── Account Types ─────────────────────────────────────────────────────────
// The four account types a new user can pick from
val accountTypes = listOf("Individual", "Joint", "Family", "Business")

// ─── Main AuthScreen ───────────────────────────────────────────────────────
// This is the whole login/signup screen — one screen, no duplicates
@Composable
fun AuthScreen(
    onLogin: (String, String) -> Unit,
    // Now signup also passes accountType to MainActivity
    onSignup: (String, String, String, String) -> Unit
) {
    // Track which mode we're in
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }

    // Form field states
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    // Account type dropdown state
    var selectedAccountType by remember { mutableStateOf("Individual") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current

    // Dark green → black gradient background (same vibe as before)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SharkBg)
    ) {

        // ── TOP BRANDING SECTION ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .padding(top = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo from drawable folder
            AsyncImage(
                model = R.drawable.sharkfinlogo,
                contentDescription = "SharkFin Logo",
                modifier = Modifier.size(120.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Fixed: "SharkFin" instead of "alamUI" — because this is SharkFin lol
            Row {
                Text(
                    text = "Shark",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light
                )
                Text(
                    text = "Fin",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Financial Growth",
                color = SharkGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp
            )
        }

        // ── KEYBOARD SLIDE ANIMATION ─────────────────────────────────────
        // When the keyboard pops up, the card slides up so nothing is hidden
        val imeInsets = WindowInsets.ime
        val keyboardHeight = imeInsets.getBottom(LocalDensity.current)
        val translationY by animateFloatAsState(
            targetValue = if (keyboardHeight > 0) -keyboardHeight * 0.4f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        )

        // ── BOTTOM CARD ───────────────────────────────────────────────────
        // The frosted glass card that holds all the form inputs
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer { this.translationY = translationY }
                .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)),
            color = SharkSurface
        ) {
            // scrollable so nothing gets cut off when signup fields are showing
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp, vertical = 40.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ── LOGIN / SIGNUP TOGGLE SLIDER ──────────────────────────
                AuthSlider(
                    selectedMode = authMode,
                    onModeChange = {
                        authMode = it
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // ── SIGNUP-ONLY FIELDS ────────────────────────────────────
                // These only show when the user switches to Sign Up mode
                if (authMode == AuthMode.SIGNUP) {

                    // Full Name bubble
                    AuthInputField(
                        value = name,
                        onValueChange = { name = it },
                        hint = "Full Name",
                        icon = Icons.Default.Person
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── ACCOUNT TYPE DROPDOWN ─────────────────────────────
                    // Styled to match the other bubbles
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SharkCardBorder, RoundedCornerShape(20.dp))
                            .background(
                                SharkBg,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { dropdownExpanded = true }
                            .padding(horizontal = 16.dp, vertical = 18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Show the currently selected account type
                            Text(
                                text = "$selectedAccountType Account",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            // Dropdown arrow icon
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Select Account Type",
                                tint = Color.White.copy(alpha = 0.4f)
                            )
                        }

                        // The actual dropdown menu that pops up
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.background(SharkSurface).border(1.dp, SharkCardBorder)
                        ) {
                            // Loop through all 4 account types and show each as an option
                            accountTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "$type Account",
                                            color = SharkLabel
                                        )
                                    },
                                    onClick = {
                                        selectedAccountType = type   // update selection
                                        dropdownExpanded = false      // close the menu
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── SHARED FIELDS (both Login and Signup) ─────────────────

                // Email bubble
                AuthInputField(
                    value = email,
                    onValueChange = { email = it },
                    hint = "Email Address",
                    icon = Icons.Default.Email
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password bubble
                AuthInputField(
                    value = password,
                    onValueChange = { password = it },
                    hint = "Password",
                    icon = Icons.Default.Lock,
                    isPassword = true
                )

                Spacer(modifier = Modifier.height(32.dp))

                // ── SUBMIT BUTTON ─────────────────────────────────────────
                Button(
                    onClick = {
                        if (authMode == AuthMode.LOGIN) {
                            // Login: just needs email + password
                            onLogin(email, password)
                        } else {
                            // Signup: passes name, email, password, AND account type
                            onSignup(name, email, password, selectedAccountType.uppercase())
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SharkGold
                    )
                ) {
                    Text(
                        text = if (authMode == AuthMode.LOGIN) "Access Account" else "Create Portfolio",
                        color = SharkBg,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Small hint text at the bottom
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

// ─── Auth Slider (Login / Sign Up toggle) ──────────────────────────────────
// The pill that slides left and right between Login and Sign Up
@Composable
fun AuthSlider(
    selectedMode: AuthMode,
    onModeChange: (AuthMode) -> Unit
) {
    // Animate the green pill sliding left (login) or right (signup)
    val transition = updateTransition(targetState = selectedMode, label = "AuthSlider")
    val offsetX by transition.animateDp(label = "pillOffset") { mode ->
        if (mode == AuthMode.LOGIN) 0.dp else 140.dp
    }

    Box(
        modifier = Modifier
            .width(280.dp)
            .height(48.dp)
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
            .padding(4.dp)
    ) {
        // The sliding green pill behind the text
        Box(
            modifier = Modifier
                .offset(x = offsetX)
                .width(136.dp)
                .fillMaxHeight()
                .background(Color(0xFF10b981), RoundedCornerShape(20.dp))
        )

        // The two tappable labels on top of the pill
        Row(modifier = Modifier.fillMaxSize()) {
            // Log In tap target
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onModeChange(AuthMode.LOGIN) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Log In",
                    color = if (selectedMode == AuthMode.LOGIN) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            // Sign Up tap target
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onModeChange(AuthMode.SIGNUP) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Sign Up",
                    color = if (selectedMode == AuthMode.SIGNUP) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ─── Reusable Input Field ───────────────────────────────────────────────────
// The bubbly text input used for name, email, and password
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthInputField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    icon: ImageVector,
    isPassword: Boolean = false   // if true, hides the text with dots
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp)),
        placeholder = {
            Text(hint, color = Color.White.copy(alpha = 0.4f))
        },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .alpha(0.4f),
                tint = Color.White
            )
        },
        // Hide text if it's a password field
        visualTransformation = if (isPassword)
            PasswordVisualTransformation()
        else
            androidx.compose.ui.text.input.VisualTransformation.None,
        // Show number pad for password, default keyboard for everything else
        keyboardOptions = if (isPassword)
            KeyboardOptions(keyboardType = KeyboardType.Password)
        else
            KeyboardOptions.Default,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.05f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
            focusedIndicatorColor = Color.Transparent,      // removes the underline
            unfocusedIndicatorColor = Color.Transparent,    // removes the underline
            disabledIndicatorColor = Color.Transparent,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        shape = RoundedCornerShape(20.dp)   // the bubble shape
    )
}