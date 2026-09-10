package com.retro.fx7000g.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Context
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontStyle
import com.retro.fx7000g.R
import com.retro.fx7000g.calc.CalculatorState

private const val PREFS_NAME = "fx7000g_settings"
private const val KEY_INSETS_ENABLED = "insets_enabled"
private const val KEY_CLASSIC_THEME = "classic_theme"
private const val KEY_LCD_CONTRAST = "lcd_contrast"
private const val KEY_KEY_VIBRATION = "key_vibration"

@Composable
fun CalculatorScreen(modifier: Modifier = Modifier) {
    val state = remember { CalculatorState() }

    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // When enabled, keep the calculator clear of the display cutout and
    // navigation bar. Tapping the "GRAPHICS" plate toggles this so devices
    // that can safely use the full screen can stretch the view edge-to-edge.
    // The choice is persisted so it survives app restarts.
    var insetsEnabled by remember {
        mutableStateOf(prefs.getBoolean(KEY_INSETS_ENABLED, true))
    }

    // Opt-in "classic" skin (crème/black keys on brushed aluminium). Defaults
    // to off so the dark theme remains the baseline and is trivially revertible.
    // Long-press the CASIO/fx-7000G branding to toggle it.
    var classicTheme by remember {
        mutableStateOf(prefs.getBoolean(KEY_CLASSIC_THEME, false))
    }
    val theme: Fx7000gTheme = if (classicTheme) ClassicTheme else DarkTheme

    // LCD contrast in [0, 1]. 0.5 = normal (strong pixel separation).
    // Persisted so the user's preferred contrast survives app restarts.
    // Adjusted by swiping left/right directly on the LCD display.
    var lcdContrast by remember {
        mutableStateOf(prefs.getFloat(KEY_LCD_CONTRAST, 0.5f))
    }

    var keyVibration by remember {
        mutableStateOf(
            prefs.getBoolean(KEY_KEY_VIBRATION, true)
        )
    }

    CompositionLocalProvider(LocalFx7000gTheme provides theme) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .then(
                    if (theme.useBodyTexture) {
                        Modifier.paint(
                            painterResource(R.drawable.aluminum),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Modifier.background(theme.body)
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (insetsEnabled) {
                            Modifier.windowInsetsPadding(
                                WindowInsets.displayCutout.union(
                                    WindowInsets.navigationBars
                                )
                            )
                        } else {
                            Modifier
                        }
                    )
                    .padding(10.dp)
                    .padding(14.dp)
            ) {
                BrandingHeader(
                    insetsEnabled = insetsEnabled,
                    onToggleGraphics = {
                        insetsEnabled = !insetsEnabled
                        prefs.edit()
                            .putBoolean(KEY_INSETS_ENABLED, insetsEnabled)
                            .apply()
                    },
                    classicTheme = classicTheme,
                    onToggleTheme = {
                        classicTheme = !classicTheme
                        prefs.edit()
                            .putBoolean(KEY_CLASSIC_THEME, classicTheme)
                            .apply()
                    },
                    keyVibration = keyVibration,
                    onToggleKeyVibration = {
                        keyVibration = !keyVibration
                        prefs.edit()
                            .putBoolean(KEY_KEY_VIBRATION, keyVibration)
                            .apply()
                    }
                )

                Spacer(Modifier.height(10.dp))

                LcdDisplay(
                    entry = state.entry,
                    result = state.result,
                    modeLabel = state.modeLabel,
                    memorySet = state.memorySet,
                    cursor = state.cursor,
                    showCursor = state.showCursor,
                    graph = state.graphBuffer,
                    rangeLines = state.rangeLines,
                    rangeCursorRow = state.rangeCursorRow,
                    rangeCursorCol = state.rangeCursorCol,
                    modeLines = state.modeLines,
                    presetLines = state.presetLines,
                    indicator = state.indicator,
                    traceCol = state.traceCol,
                    traceRow = state.traceRow,
                    traceText = state.traceText,
                    progState = state.progState,
                    contrast = lcdContrast,
                    onContrastChange = { newContrast ->
                        lcdContrast = newContrast
                        prefs.edit().putFloat(KEY_LCD_CONTRAST, newContrast).apply()
                    }
                )

                Spacer(Modifier.height(16.dp))

                Keypad(
                    state = state,
                    keyVibration = keyVibration,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BrandingHeader(
    onToggleGraphics: () -> Unit = {},
    onToggleTheme: () -> Unit = {},
    onToggleKeyVibration: () -> Unit = {},
    keyVibration: Boolean,
    classicTheme: Boolean,
    insetsEnabled: Boolean,
) {
    val theme = LocalFx7000gTheme.current
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Only the CASIO / fx-7000G branding is clickable.
            Box(
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    menuExpanded = true
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CASIO",
                        color = theme.branding,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif
                    )

                    Spacer(Modifier.width(10.dp))
//
//                    Text(
//                        text = "SCIENTIFIC CALCULATOR",
//                        color = theme.branding,
//                        fontSize = 11.sp,
//                        fontWeight = FontWeight.Medium,
//                        fontFamily = FontFamily.SansSerif,
//                        modifier = Modifier.offset(y = 3.dp)
//                    )

//                    Spacer(Modifier.width(28.dp))

                    Text(
                        text = "fx-7000G",
                        color = theme.branding,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        fontStyle = FontStyle.Italic,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(theme.modelPlate)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "GRAPHICS",
                    color = Fx7000gColors.Branding,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { 
                    Text(
                        if (classicTheme) {
                            "Dark theme OFF"
                        } else {
                            "Dark theme ON"
                        }
                    )
                },
                onClick = {
                    onToggleTheme()
                    menuExpanded = false
                }
            )

            DropdownMenuItem(
                text = { 
                    Text(
                        if (insetsEnabled) {
                            "Full height OFF"
                        } else {
                            "Full height ON"
                        }
                    )
                },
                onClick = {
                    onToggleGraphics()
                    menuExpanded = false
                }
            )

            DropdownMenuItem(
                text = {
                    Text(
                        if (keyVibration) {
                            "Key haptics ON"
                        } else {
                            "Key haptics OFF"
                        }
                    )
                },
                onClick = {
                    onToggleKeyVibration()
                    menuExpanded = false
                }
            )
        }
    }
}
