package com.rr.numio.notes.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.notes.R
import com.rr.numio.notes.ui.theme.NumioBackground
import com.rr.numio.notes.ui.theme.NumioOnAccent
import com.rr.numio.notes.ui.theme.NumioText
import com.rr.numio.notes.ui.theme.NumioTextFaint
import com.rr.numio.notes.ui.theme.NumioYellow

// Shown instead of the notes while the app is locked; asks to unlock straight away
@Composable
fun LockScreen(onUnlock: () -> Unit) {
    LaunchedEffect(Unit) { onUnlock() }
    Column(
        Modifier.fillMaxSize().background(NumioBackground).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(112.dp).clip(RoundedCornerShape(30.dp)).background(Color(0xFF141414)),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.ic_launcher_foreground), null, modifier = Modifier.size(150.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Notes is locked", color = NumioText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text("Use your fingerprint or screen lock", color = NumioTextFaint, fontSize = 14.sp)
        Spacer(Modifier.height(28.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(NumioYellow)
                .clickable(onClick = onUnlock)
                .padding(horizontal = 36.dp, vertical = 14.dp),
        ) {
            Text("Unlock", color = NumioOnAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
