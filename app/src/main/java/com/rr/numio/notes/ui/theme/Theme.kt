package com.rr.numio.notes.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NumioDarkScheme = darkColorScheme(
    primary = NumioYellow,
    onPrimary = NumioBackground,
    background = NumioBackground,
    onBackground = NumioText,
    surface = NumioCard,
    onSurface = NumioText,
    surfaceVariant = NumioButton,
    onSurfaceVariant = NumioTextSoft,
    outline = NumioDivider,
)

// Dark only for now. Light / follow-system comes with the Settings DataStore step.
@Composable
fun NotesbyNumioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NumioDarkScheme,
        typography = Typography,
        content = content,
    )
}
