package com.rr.numio.notes.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.notes.R
import com.rr.numio.notes.ui.theme.NumioBackground
import com.rr.numio.notes.ui.theme.NumioCard
import com.rr.numio.notes.ui.theme.NumioDivider
import com.rr.numio.notes.ui.theme.NumioText
import com.rr.numio.notes.ui.theme.NumioTextFaint
import com.rr.numio.notes.ui.theme.NumioYellow
import kotlinx.coroutines.delay

private const val REPO_URL = "https://github.com/rag-creation/numio-notes"

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0.0"
    }
    var taps by remember { mutableIntStateOf(0) }
    var showEgg by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    Box(Modifier.fillMaxSize().background(NumioBackground)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_back), "Back", tint = NumioText)
                }
                Text("Settings", color = NumioText, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }

            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("About")
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(NumioCard)) {
                    SettingRow("Version", "Tap for a surprise", value = version) {
                        taps++
                        val left = 7 - taps
                        when {
                            taps >= 7 -> { taps = 0; showEgg = true }
                            taps >= 3 -> Toast.makeText(
                                context,
                                if (left == 1) "1 more tap…" else "$left more taps…",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                    Divider()
                    SettingRow("Report an issue", "Opens GitHub Issues") {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("$REPO_URL/issues")))
                    }
                    Divider()
                    SettingRow("Source code", "GPL-3.0") {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL)))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Made by a Keralite 💛",
                    color = NumioTextFaint,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (showEgg) EasterEgg(onClose = { showEgg = false })
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = NumioYellow,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun SettingRow(title: String, subtitle: String, value: String? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = NumioText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = NumioTextFaint, fontSize = 12.sp)
        }
        if (value != null) {
            Text(value, color = NumioYellow, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Divider() {
    Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(1.dp).background(NumioDivider))
}

// Same Numio easter egg: letter-by-letter reveal, then the signature
@Composable
private fun EasterEgg(onClose: () -> Unit) {
    val line1 = "Thanks for choosing Numio 💛"
    val line2 = "With Lo❤️e, R.R"
    var shown1 by remember { mutableIntStateOf(0) }
    var shown2 by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        for (i in 1..line1.length) { shown1 = i; delay(55) }
        delay(400)
        for (i in 1..line2.length) { shown2 = i; delay(70) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xF0080808))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose,
            )
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.width(64.dp).height(3.dp).background(NumioYellow, RoundedCornerShape(2.dp)))
        Spacer(Modifier.height(18.dp))
        Text(
            line1.take(shown1),
            color = NumioText,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Text(line2.take(shown2), color = NumioYellow, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))
        Text("Tap anywhere to close", color = NumioTextFaint, fontSize = 12.sp)
    }
}
