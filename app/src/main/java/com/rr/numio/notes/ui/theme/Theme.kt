package com.rr.numio.notes.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun NotesbyNumioTheme(palette: NotesPalette, content: @Composable () -> Unit) {
    val base = if (palette.isLight) lightColorScheme() else darkColorScheme()
    val scheme = base.copy(
        primary = palette.accent,
        onPrimary = palette.onAccent,
        background = palette.background,
        onBackground = palette.text,
        surface = palette.card,
        onSurface = palette.text,
        surfaceVariant = palette.button,
        onSurfaceVariant = palette.textSoft,
        outline = palette.divider,
    )
    CompositionLocalProvider(LocalNotesPalette provides palette) {
        MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
    }
}
