package com.example.sharkfin.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SharkCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SharkSurface, RoundedCornerShape(20.dp))
            .border(1.dp, SharkBorderMedium, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 18.dp),
        content = content
    )
}

@Composable
fun SharkCardElevated(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SharkSurfaceHigh, RoundedCornerShape(24.dp))
            .border(1.dp, SharkGold.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(SharkGold)
                .align(Alignment.TopCenter)
        )
        Column(
            modifier = Modifier.padding(24.dp),
            content = content
        )
    }
}

@Composable
fun SharkCardGold(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SharkGold.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .border(1.dp, SharkGold.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        content = content
    )
}

@Composable
fun SharkButtonPrimary(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "buttonScale"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .height(54.dp)
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SharkGold,
            contentColor = SharkBase,
            disabledContainerColor = SharkGoldDim,
            disabledContentColor = SharkBase
        ),
        interactionSource = interactionSource,
        enabled = enabled,
        elevation = null
    ) {
        Text(
            text = text,
            style = SharkTypography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
        )
    }
}

@Composable
fun SharkButtonSecondary(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "buttonScale"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .height(54.dp)
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SharkSurfaceHigh,
            contentColor = SharkTextPrimary,
            disabledContainerColor = SharkSurface,
            disabledContentColor = SharkTextMuted
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, SharkBorderStrong),
        interactionSource = interactionSource,
        enabled = enabled,
        elevation = null
    ) {
        Text(
            text = text,
            style = SharkTypography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
        )
    }
}

@Composable
fun SharkButtonGhost(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    ) {
        Text(
            text = text,
            style = SharkTypography.bodyLarge.copy(color = SharkGold)
        )
    }
}

@Composable
fun SharkIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SharkSurfaceHigh)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (isActive) SharkGold else SharkTextSecondary
        )
    }
}

@Composable
fun SharkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    readOnly: Boolean = false,
    isError: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = when {
        isError -> SharkNegative
        isFocused -> SharkGold
        else -> SharkBorderMedium
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(SharkSurface, RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp),
        textStyle = SharkTypography.bodyLarge.copy(color = SharkTextPrimary, fontSize = 15.sp),
        cursorBrush = SolidColor(SharkGold),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        readOnly = readOnly,
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxHeight()
            ) {
                if (leadingIcon != null) {
                    Icon(
                        leadingIcon,
                        contentDescription = null,
                        tint = if (isFocused) SharkGold else SharkTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = hint,
                            style = SharkTypography.bodyLarge.copy(fontSize = 15.sp),
                            color = SharkTextMuted
                        )
                    }
                    innerTextField()
                }
            }
        }
    )
}

@Composable
fun SharkSectionHeader(
    title: String,
    onSeeAllClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            color = SharkTextMuted,
            style = SharkTypography.titleSmall
        )
        if (onSeeAllClick != null) {
            Text(
                text = "See all",
                modifier = Modifier.clickable { onSeeAllClick() },
                color = SharkGold,
                style = SharkTypography.labelMedium.copy(fontSize = 11.sp)
            )
        }
    }
}
