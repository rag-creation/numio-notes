package com.rr.numio.notes.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.rr.numio.notes.R

// Default Material 3 type for the app UI
val Typography = Typography()

// Note text uses Inter (SIL OFL 1.1) with real bold and italic files, so B and I always
// show, whatever the phone's system font is. Black is there for phones with "Bold font"
// turned on: normal text becomes bold there, and bold text becomes black.
// Malayalam and emoji fall back to the system fonts.
val NotesFont = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_regular_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_bold_italic, FontWeight.Bold, FontStyle.Italic),
    Font(R.font.inter_black, FontWeight.Black),
    Font(R.font.inter_black_italic, FontWeight.Black, FontStyle.Italic),
)
