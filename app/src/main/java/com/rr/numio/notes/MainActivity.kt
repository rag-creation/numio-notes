package com.rr.numio.notes

import android.os.Bundle
import android.view.WindowManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.rr.numio.notes.ui.LockScreen
import com.rr.numio.notes.ui.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontWeight
import com.rr.numio.notes.ui.theme.NumioAccentInk
import com.rr.numio.notes.ui.theme.NumioCard
import com.rr.numio.notes.ui.theme.NumioText
import com.rr.numio.notes.ui.theme.NumioTextSoft
import com.rr.numio.notes.ui.trash.TrashScreen
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rr.numio.notes.data.AppSettings
import com.rr.numio.notes.ui.theme.Palettes
import kotlinx.coroutines.launch
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rr.numio.notes.ui.NotesViewModel
import com.rr.numio.notes.ui.drawing.DrawingScreen
import com.rr.numio.notes.ui.editor.EditorScreen
import com.rr.numio.notes.ui.editor.TodoScreen
import com.rr.numio.notes.ui.list.NoteListScreen
import com.rr.numio.notes.ui.settings.SettingsScreen
import com.rr.numio.notes.ui.theme.NotesbyNumioTheme

class MainActivity : FragmentActivity() {

    // App lock: unlocked for this visit; locks again after a minute away
    private var unlocked by mutableStateOf(false)
    private var stoppedAt = 0L

    override fun onStop() {
        super.onStop()
        stoppedAt = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        if (stoppedAt > 0 && System.currentTimeMillis() - stoppedAt > 60_000) unlocked = false
    }

    // Fingerprint, face or the phone's PIN / pattern
    private fun authenticate(title: String, onSuccess: () -> Unit) {
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .build()
        prompt.authenticate(info)
    }

    private fun lockAvailable(): Boolean =
        BiometricManager.from(this).canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appSettings = AppSettings(applicationContext)
        // A crash report from last time, if the app closed unexpectedly
        val crashFile = NotesApp.crashFile(this)
        var crashReport by mutableStateOf(runCatching { crashFile.takeIf { it.exists() }?.readText() }.getOrNull())
        setContent {
            // Wait for the saved theme (a few ms) so a light theme never flashes dark first
            val mode by appSettings.mode.collectAsState(initial = null)
            val themeId by appSettings.theme.collectAsState(initial = null)
            val lockSetting by appSettings.appLock.collectAsState(initial = null)
            val blockShots by appSettings.blockScreenshots.collectAsState(initial = false)
            // Block screenshots / screen recording when chosen in Settings
            SideEffect {
                if (blockShots) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
            val m = mode ?: return@setContent
            val t = themeId ?: return@setContent
            val lockOn = lockSetting ?: return@setContent
            val palette = Palettes.resolve(t, m, isSystemInDarkTheme())
            val scope = rememberCoroutineScope()

            // Status and navigation bar icons dark on light themes, white on dark ones
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = palette.isLight
                    isAppearanceLightNavigationBars = palette.isLight
                }
            }

            NotesbyNumioTheme(palette) {
                crashReport?.let { report ->
                    AlertDialog(
                        onDismissRequest = { crashFile.delete(); crashReport = null },
                        containerColor = NumioCard,
                        titleContentColor = NumioText,
                        textContentColor = NumioTextSoft,
                        title = { Text("Notes closed unexpectedly", fontWeight = FontWeight.Bold) },
                        text = { Text("Send the error report so it can be fixed? It only has the error details, none of your notes.") },
                        confirmButton = {
                            TextButton(onClick = {
                                Share.send(this@MainActivity, report, "Notes by Numio crash report")
                                crashFile.delete(); crashReport = null
                            }) { Text("Send report", color = NumioAccentInk, fontWeight = FontWeight.Bold) }
                        },
                        dismissButton = {
                            TextButton(onClick = { crashFile.delete(); crashReport = null }) { Text("Not now", color = NumioTextSoft) }
                        },
                    )
                }
                if (lockOn && lockAvailable() && !unlocked) {
                    LockScreen(onUnlock = { authenticate("Unlock Notes") { unlocked = true } })
                    return@NotesbyNumioTheme
                }
                val nav = rememberNavController()
                val notesViewModel: NotesViewModel = viewModel()

                NavHost(navController = nav, startDestination = "list") {
                    composable("list") {
                        NoteListScreen(
                            viewModel = notesViewModel,
                            onOpenNote = { note ->
                                nav.navigate(
                                    when {
                                        note.isDrawing -> "drawing/${note.id}"
                                        note.isTodo -> "todo/${note.id}"
                                        else -> "editor/${note.id}"
                                    }
                                )
                            },
                            onNewNote = { nav.navigate("editor/0") },
                            onNewTodo = { nav.navigate("todo/0") },
                            onNewDrawing = { nav.navigate("drawing/0") },
                            onOpenSettings = { nav.navigate("settings") },
                        )
                    }
                    composable(
                        "editor/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { entry ->
                        EditorScreen(
                            noteId = entry.arguments?.getLong("id") ?: 0L,
                            viewModel = notesViewModel,
                            onDone = { nav.popBackStack() },
                        )
                    }
                    composable(
                        "todo/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { entry ->
                        TodoScreen(
                            noteId = entry.arguments?.getLong("id") ?: 0L,
                            viewModel = notesViewModel,
                            onDone = { nav.popBackStack() },
                        )
                    }
                    composable(
                        "drawing/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { entry ->
                        DrawingScreen(
                            noteId = entry.arguments?.getLong("id") ?: 0L,
                            viewModel = notesViewModel,
                            onDone = { nav.popBackStack() },
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            mode = m,
                            themeId = t,
                            onModeChange = { scope.launch { appSettings.setMode(it) } },
                            onThemeChange = { scope.launch { appSettings.setTheme(it) } },
                            importState = notesViewModel.importState.collectAsState().value,
                            onImport = { notesViewModel.import(it) },
                            onImportSeen = { notesViewModel.clearImportState(); notesViewModel.clearBackupState() },
                            backupState = notesViewModel.backupState.collectAsState().value,
                            onBackup = { notesViewModel.backupTo(it) },
                            onExport = { notesViewModel.exportMarkdownTo(it) },
                            trashCount = notesViewModel.trash.collectAsState().value.size,
                            onOpenTrash = { nav.navigate("trash") },
                            appLock = lockOn,
                            lockAvailable = lockAvailable(),
                            onAppLockChange = { on ->
                                // Confirm with the lock itself, both to turn it on and off
                                authenticate(if (on) "Turn on app lock" else "Turn off app lock") {
                                    unlocked = true
                                    scope.launch { appSettings.setAppLock(on) }
                                }
                            },
                            blockScreenshots = blockShots,
                            onBlockScreenshotsChange = { on -> scope.launch { appSettings.setBlockScreenshots(on) } },
                            onBack = { nav.popBackStack() },
                        )
                    }
                    composable("trash") {
                        TrashScreen(viewModel = notesViewModel, onBack = { nav.popBackStack() })
                    }
                }
            }
        }
    }
}
