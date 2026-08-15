package com.familyos.feature.notes.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.familyos.feature.notes.ui.NoteEditorScreen
import com.familyos.feature.notes.ui.NotesListScreen
import com.familyos.feature.notes.viewmodel.NotesViewModel

/** Notes route constants. */
object NotesRoutes {
    const val LIST = "notes"
    const val EDITOR = "notes/editor/{noteId}"
    const val NEW = "notes/editor/new"

    fun editor(noteId: String) = "notes/editor/$noteId"
}

/** Registers notes destinations. */
fun NavGraphBuilder.notesGraph(navController: NavHostController) {
    composable(NotesRoutes.LIST) { NotesListRoute(navController) }
    composable(NotesRoutes.NEW) { NoteEditorRoute(navController, noteId = null) }
    composable(
        route = NotesRoutes.EDITOR,
        arguments = listOf(navArgument("noteId") { type = NavType.StringType }),
    ) { entry ->
        val id = entry.arguments?.getString("noteId")
        NoteEditorRoute(navController, noteId = id?.takeIf { it != "new" })
    }
}

@Composable
private fun NotesListRoute(navController: NavHostController) {
    val vm: NotesViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    NotesListScreen(
        state = state,
        onOpenNote = { navController.navigate(NotesRoutes.editor(it)) },
        onCreate = {
            vm.startNewNote()
            navController.navigate(NotesRoutes.NEW)
        },
        onQueryChange = vm::setQuery,
        onShowArchivedChange = vm::setShowArchived,
    )
}

@Composable
private fun NoteEditorRoute(navController: NavHostController, noteId: String?) {
    val vm: NotesViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(noteId) {
        if (noteId == null) vm.startNewNote() else vm.loadNote(noteId)
    }
    NoteEditorScreen(
        note = state.selected,
        errorMessage = state.errorMessage,
        onBack = { navController.popBackStack() },
        onTitleChange = { vm.updateDraft(title = it) },
        onBodyChange = { vm.updateDraft(body = it) },
        onTagsChange = { vm.updateDraft(tags = it) },
        onAddPhotoUrl = { url ->
            val current = state.selected?.photoUrls.orEmpty()
            vm.updateDraft(photoUrls = current + url)
        },
        onAddChecklistItem = vm::addChecklistItem,
        onToggleChecklist = vm::toggleChecklistItem,
        onSave = {
            vm.save()
            navController.popBackStack()
        },
        onDelete = vm::delete,
        onTogglePin = vm::togglePin,
        onToggleArchive = vm::toggleArchive,
    )
}
