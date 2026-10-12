package com.rr.numio.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import com.rr.numio.notes.ui.editor.EditorScreen
import com.rr.numio.notes.ui.editor.TodoScreen
import com.rr.numio.notes.ui.list.NoteListScreen
import com.rr.numio.notes.ui.settings.SettingsScreen
import com.rr.numio.notes.ui.theme.NotesbyNumioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appSettings = AppSettings(applicationContext)
        setContent {
            // Wait for the saved theme (a few ms) so a light theme never flashes dark first
            val mode by appSettings.mode.collectAsState(initial = null)
            val themeId by appSettings.theme.collectAsState(initial = null)
            val m = mode ?: return@setContent
            val t = themeId ?: return@setContent
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
                val nav = rememberNavController()
                val notesViewModel: NotesViewModel = viewModel()

                NavHost(navController = nav, startDestination = "list") {
                    composable("list") {
                        NoteListScreen(
                            viewModel = notesViewModel,
                            onOpenNote = { note ->
                                nav.navigate(if (note.isTodo) "todo/${note.id}" else "editor/${note.id}")
                            },
                            onNewNote = { nav.navigate("editor/0") },
                            onNewTodo = { nav.navigate("todo/0") },
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
                    composable("settings") {
                        SettingsScreen(
                            mode = m,
                            themeId = t,
                            onModeChange = { scope.launch { appSettings.setMode(it) } },
                            onThemeChange = { scope.launch { appSettings.setTheme(it) } },
                            importState = notesViewModel.importState.collectAsState().value,
                            onImport = { notesViewModel.import(it) },
                            onImportSeen = { notesViewModel.clearImportState() },
                            onBack = { nav.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
