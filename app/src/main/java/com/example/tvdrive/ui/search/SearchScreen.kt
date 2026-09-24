package com.example.tvdrive.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import com.example.tvdrive.ui.components.GlassButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tvdrive.LocalAppContainer
import com.example.tvdrive.data.model.DriveFile
import com.example.tvdrive.data.repository.DriveRepository
import com.example.tvdrive.theme.*
import com.example.tvdrive.ui.components.AmbientGlowBackground
import com.example.tvdrive.ui.components.TvFocusableItem
import com.example.tvdrive.ui.components.iconColor
import com.example.tvdrive.ui.components.iconVector
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ── ViewModel ─────────────────────────────────────────────────────────────────

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Results(val files: List<DriveFile>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

class SearchViewModel(private val driveRepository: DriveRepository) : ViewModel() {
    private val _state = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val state: StateFlow<SearchUiState> = _state

    private var debounceJob: Job? = null

    fun search(query: String) {
        if (query.isBlank()) { _state.value = SearchUiState.Idle; return }
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(500) // debounce 500ms — prevents API spam while user types
            _state.value = SearchUiState.Loading
            driveRepository.searchFiles(query)
                .onSuccess { _state.value = SearchUiState.Results(it) }
                .onFailure { _state.value = SearchUiState.Error(it.message ?: "Search failed") }
        }
    }

    fun streamUrl(fileId: String) = driveRepository.streamUrl(fileId)

    class Factory(private val repo: DriveRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SearchViewModel(repo) as T
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun SearchScreen(
    onOpenFolder: (String, String) -> Unit,
    onPlayVideo: (String, String, String) -> Unit,
    onPlayAudio: (String, String, String) -> Unit,
    onViewImage: (List<String>, Int, String) -> Unit,
    onOpenPdf: (String, String) -> Unit = { _, _ -> },
    onBack: () -> Unit
) {
    val container = LocalAppContainer.current
    val vm: SearchViewModel = viewModel(factory = SearchViewModel.Factory(container.driveRepository))
    val state by vm.state.collectAsState()
    val focusManager = LocalFocusManager.current
    var query by remember { mutableStateOf("") }
    var unsupportedFile by remember { mutableStateOf<DriveFile?>(null) }

    AmbientGlowBackground {
        Column(Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(width = 1.dp, color = Color(0xFFE2E8F0))
                    .padding(horizontal = 32.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TvFocusableItem(onClick = onBack, cornerRadius = 8.dp) { focused ->
                    Row(
                        modifier = Modifier
                            .background(
                                color = if (focused) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = if (focused) 2.dp else 1.dp,
                                color = if (focused) Color(0xFF2563EB) else Color(0xFFCBD5E1),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = if (focused) Color(0xFF2563EB) else Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Back",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (focused) Color(0xFF2563EB) else Color(0xFF334155),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; vm.search(it) },
                    placeholder = { Text("Search files & folders...", color = Color(0xFF94A3B8)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = Color(0xFF64748B)
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF1E293B),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor = Color(0xFF2563EB)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { vm.search(query) }),
                    modifier = Modifier
                        .weight(1f)
                        .onPreviewKeyEvent { event ->
                            if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN &&
                                event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN
                            ) {
                                focusManager.moveFocus(FocusDirection.Down)
                                true
                            } else false
                        }
                )
            }

            when (val s = state) {
                is SearchUiState.Idle -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "Type on the virtual keyboard to search your Drive",
                            color = Color(0xFF64748B),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                is SearchUiState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF2563EB))
                }
                is SearchUiState.Error -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = "Error",
                            tint = Color(0xFFDC2626)
                        )
                        Text(
                            text = s.message,
                            color = Color(0xFFDC2626),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                is SearchUiState.Results -> {
                    if (s.files.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No results found for \"$query\"",
                                color = Color(0xFF64748B),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(28.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text(
                                    text = "${s.files.size} results found",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                            items(s.files, key = { it.id }) { file ->
                                SearchResultRow(
                                    file = file,
                                    onClick = {
                                        when {
                                            file.isFolder -> onOpenFolder(file.id, file.name)
                                            file.isVideo  -> onPlayVideo(vm.streamUrl(file.id), file.id, file.name)
                                            file.isAudio  -> onPlayAudio(vm.streamUrl(file.id), file.id, file.name)
                                            file.isImage  -> onViewImage(listOf(vm.streamUrl(file.id)), 0, file.name)
                                            file.isPdf    -> onOpenPdf(file.id, file.name)
                                            else -> { unsupportedFile = file }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Unsupported File Dialog
        unsupportedFile?.let { file ->
            val ext = file.name.substringAfterLast('.', "").uppercase()
            val typeLabel = when {
                ext.isNotEmpty() -> "$ext format"
                file.mimeType.contains("spreadsheet") || file.mimeType.contains("excel") -> "Spreadsheet"
                file.mimeType.contains("presentation") || file.mimeType.contains("powerpoint") -> "Presentation"
                file.mimeType.contains("document") || file.mimeType.contains("word") -> "Word Document"
                else -> "this type of"
            }
            AlertDialog(
                onDismissRequest = { unsupportedFile = null },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                containerColor = Color.White,
                icon = {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFFFEF2F2), androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "File Format Unsupported",
                        color = Color(0xFF0F172A),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "We currently don't support $typeLabel files (\"${file.name}\").",
                            color = Color(0xFF334155),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "NovaDrive TV supports Videos, Music/Audio, Photos, and PDF documents.",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }
                },
                confirmButton = {
                    GlassButton(
                        text = "OK",
                        isPrimary = true,
                        onClick = { unsupportedFile = null }
                    )
                }
            )
        }
    }
}

@Composable
private fun SearchResultRow(file: DriveFile, onClick: () -> Unit) {
    TvFocusableItem(onClick = onClick, cornerRadius = 12.dp, modifier = Modifier.fillMaxWidth()) { focused ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (focused) Color(0xFFEFF6FF) else Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = if (focused) 2.5.dp else 1.dp,
                    color = if (focused) Color(0xFF2563EB) else Color(0xFFE2E8F0),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = file.fileCategory.iconVector,
                contentDescription = null,
                tint = file.fileCategory.iconColor,
                modifier = Modifier.size(28.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = file.mimeType,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    maxLines = 1
                )
            }
        }
    }
}
