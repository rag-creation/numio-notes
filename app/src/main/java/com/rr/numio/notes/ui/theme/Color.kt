package com.rr.numio.notes.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Screens use these names; they read the active theme's palette.
val NumioBackground: Color @Composable @ReadOnlyComposable get() = LocalNotesPalette.current.background
val NumioCard: Color @Composable @ReadOnlyComposable get() = LocalNotesPalette.current.card
val NumioButton: Color @Composable @ReadOnlyComposable get() = LocalNotesPalette.current.button
val NumioRaised: Color @Composable @ReadOnlyComposable get() = LocalNotesPalette.current.raised
val NumioDivider: Color @Composable @ReadOnlyComposable get() = LocalNotesPalette.current.divider
val NumioTrack: Color @Composable @ReadOnlyComposable get() = LocalNotesPalette.current.track
val NumioText: Color @Composable @ReadOnlyComposable get() = LocalNotesPalette.current.text
val NumioTextBody: Color @Composable @ReadOnlyComposable get() = LocalNotesPalette.current.textBody
val NumioTextSoft: Color @Composable @ReadOnlyComposable get() = LocalNotesPalette.current.textSoft
val NumioTextFaint: Color @Composable @ReadOnlyComposable get() = LocalNotesPalette.current.textFaint
val NumioCheckBorder: Color @Composable @ReadOnlyComposable get() = LocalNotesPalette.current.checkBorder
val NumioYellow: Color @Composable @ReadOnlyComposable get() = LocalNotesPalette.current.accent
val NumioAccentInk: Color @Composable @ReadOnlyComposable get() = LocalNotesPalette.current.accentInk
val NumioOnAccent: Color @Composable @ReadOnlyComposable get() = LocalNotesPalette.current.onAccent
val NumioAccentSoft: Color @Composable @ReadOnlyComposable get() = LocalNotesPalette.current.accentSoft
