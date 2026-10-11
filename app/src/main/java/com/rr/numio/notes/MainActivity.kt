package com.rr.numio.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rr.numio.notes.ui.NotesViewModel
import com.rr.numio.notes.ui.editor.EditorScreen
import com.rr.numio.notes.ui.list.NoteListScreen
import com.rr.numio.notes.ui.settings.SettingsScreen
import com.rr.numio.notes.ui.theme.NotesbyNumioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotesbyNumioTheme {
                val nav = rememberNavController()
                val notesViewModel: NotesViewModel = viewModel()

                NavHost(navController = nav, startDestination = "list") {
                    composable("list") {
                        NoteListScreen(
                            viewModel = notesViewModel,
                            onOpenNote = { id -> nav.navigate("editor/$id") },
                            onNewNote = { nav.navigate("editor/0") },
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
                    composable("settings") {
                        SettingsScreen(onBack = { nav.popBackStack() })
                    }
                }
            }
        }
    }
}
