package com.rr.numio.notes.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Every colour the screens use. One palette per theme.
data class NotesPalette(
    val id: String,
    val name: String,
    val background: Color,
    val card: Color,
    val button: Color,      // round top-bar buttons, format pill
    val raised: Color,      // + menu options
    val divider: Color,
    val track: Color,       // progress bar track, open FAB
    val text: Color,
    val textBody: Color,
    val textSoft: Color,
    val textFaint: Color,
    val checkBorder: Color,
    val accent: Color,      // fills: FAB, ticks, glow line, progress
    val accentInk: Color,   // accent used as text or a thin icon
    val onAccent: Color,    // text / icons on top of an accent fill
    val accentSoft: Color,  // subtle accent background (active format button)
    val isLight: Boolean,
)

object Palettes {
    val NumioDark = NotesPalette(
        id = "numio", name = "Numio",
        background = Color(0xFF0F0F0F), card = Color(0xFF1A1A1A), button = Color(0xFF1C1C1C),
        raised = Color(0xFF1F1F1F), divider = Color(0xFF262626), track = Color(0xFF2A2A2A),
        text = Color(0xFFEDEDED), textBody = Color(0xFFD6D6D6), textSoft = Color(0xFFA8A8A8),
        textFaint = Color(0xFF8A8A8A), checkBorder = Color(0xFF6A6A6A),
        accent = Color(0xFFFFC107), accentInk = Color(0xFFFFC107), onAccent = Color(0xFF111111),
        accentSoft = Color(0xFF2A2410), isLight = false,
    )

    // Same theme in light mode: warm paper, darker amber where yellow would be text
    val NumioLight = NotesPalette(
        id = "numio", name = "Numio",
        background = Color(0xFFF2F1EC), card = Color(0xFFFFFFFF), button = Color(0xFFE6E5DF),
        raised = Color(0xFFFFFFFF), divider = Color(0xFFE0DED7), track = Color(0xFFE0DED7),
        text = Color(0xFF1A1A1A), textBody = Color(0xFF2E2E2E), textSoft = Color(0xFF555555),
        textFaint = Color(0xFF767676), checkBorder = Color(0xFF9A9A9A),
        accent = Color(0xFFF5B800), accentInk = Color(0xFF8A6300), onAccent = Color(0xFF111111),
        accentSoft = Color(0xFFFFF1C2), isLight = true,
    )

    // Palette themes, same names and colours as Ezhuthola and Clock by Numio
    val Bloom = NotesPalette(
        id = "bloom", name = "Bloom",
        background = Color(0xFF021861), card = Color(0xFF0B2775), button = Color(0xFF0F2C80),
        raised = Color(0xFF0F2C80), divider = Color(0xFF1A3A8F), track = Color(0xFF082069),
        text = Color(0xFFF4F1EA), textBody = Color(0xFFDCE3F5), textSoft = Color(0xFFB5C3EA),
        textFaint = Color(0xFF93A6DC), checkBorder = Color(0xFF93A6DC),
        accent = Color(0xFF4A7DE6), accentInk = Color(0xFF8FB3FF), onAccent = Color(0xFFFFFFFF),
        accentSoft = Color(0xFF133A9E), isLight = false,
    )
    val BloomRed = NotesPalette(
        id = "bloomred", name = "Bloom Red",
        background = Color(0xFFDE1110), card = Color(0xFFB80E0D), button = Color(0xFF8E0A0A),
        raised = Color(0xFF8E0A0A), divider = Color(0xFFC9100F), track = Color(0xFF8E0A0A),
        text = Color(0xFFFFF6F2), textBody = Color(0xFFFFE9E4), textSoft = Color(0xFFFFD7D1),
        textFaint = Color(0xFFFFC2B8), checkBorder = Color(0xFFFFD7D1),
        accent = Color(0xFF021861), accentInk = Color(0xFFFFFFFF), onAccent = Color(0xFFFFFFFF),
        accentSoft = Color(0xFF8E0A0A), isLight = false,
    )
    val CosmicLatte = NotesPalette(
        id = "latte", name = "Cosmic Latte",
        background = Color(0xFFE8DCC6), card = Color(0xFFFBF6EC), button = Color(0xFFF3EBDD),
        raised = Color(0xFFFBF6EC), divider = Color(0xFFD8CBB3), track = Color(0xFFD8CBB3),
        text = Color(0xFF3B1C21), textBody = Color(0xFF4A2A2F), textSoft = Color(0xFF6E5D50),
        textFaint = Color(0xFF85756A), checkBorder = Color(0xFFB7A89A),
        accent = Color(0xFF722F37), accentInk = Color(0xFF722F37), onAccent = Color(0xFFFBF6EC),
        accentSoft = Color(0xFFEADBD2), isLight = true,
    )
    val Sage = NotesPalette(
        id = "sage", name = "Sage",
        background = Color(0xFF9CAF88), card = Color(0xFFF4F6F0), button = Color(0xFFE8ECE1),
        raised = Color(0xFFF4F6F0), divider = Color(0xFFC9D3BD), track = Color(0xFFC9D3BD),
        text = Color(0xFF1F2A1A), textBody = Color(0xFF2A3524), textSoft = Color(0xFF33402B),
        textFaint = Color(0xFF4A5741), checkBorder = Color(0xFF7C8A70),
        accent = Color(0xFFA34500), accentInk = Color(0xFFA34500), onAccent = Color(0xFFFFF4EA),
        accentSoft = Color(0xFFF3E2D3), isLight = true,
    )
    val PocketAtlas = NotesPalette(
        id = "atlas", name = "Pocket Atlas",
        background = Color(0xFFB7D8F5), card = Color(0xFFFFFFFF), button = Color(0xFFFFD700),
        raised = Color(0xFFFFFFFF), divider = Color(0xFFD6E6F5), track = Color(0xFFD6E6F5),
        text = Color(0xFF1A2533), textBody = Color(0xFF24344A), textSoft = Color(0xFF2F4A66),
        textFaint = Color(0xFF4A6480), checkBorder = Color(0xFF7A93AD),
        accent = Color(0xFFD00500), accentInk = Color(0xFFD00500), onAccent = Color(0xFFFFFFFF),
        accentSoft = Color(0xFFFFE3E0), isLight = true,
    )

    // Order shown in Settings
    val themes = listOf(NumioDark, Bloom, BloomRed, CosmicLatte, Sage, PocketAtlas)

    // Numio follows Dark / Light / System; palette themes have their own colours
    fun resolve(themeId: String, mode: String, systemDark: Boolean): NotesPalette {
        if (themeId != "numio") return themes.firstOrNull { it.id == themeId } ?: NumioDark
        val dark = when (mode) {
            "dark" -> true
            "light" -> false
            else -> systemDark
        }
        return if (dark) NumioDark else NumioLight
    }
}

val LocalNotesPalette = staticCompositionLocalOf { Palettes.NumioDark }
