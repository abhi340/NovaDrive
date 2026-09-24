package com.example.tvdrive.ui.drive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tvdrive.data.model.DriveFile
import com.example.tvdrive.data.repository.DriveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class DriveUiState {
    object Loading : DriveUiState()
    data class Success(val files: List<DriveFile>, val canLoadMore: Boolean = false) : DriveUiState()
    data class Error(val message: String) : DriveUiState()
}

class DriveBrowserViewModel(
    private val repository: DriveRepository,
    val folderId: String,
    val folderName: String
) : ViewModel() {

    private val _state = MutableStateFlow<DriveUiState>(DriveUiState.Loading)
    val state: StateFlow<DriveUiState> = _state

    init { loadFiles() }

    fun loadFiles(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _state.value = DriveUiState.Loading
            repository.listFiles(folderId, forceRefresh)
                .onSuccess { _state.value = DriveUiState.Success(it) }
                .onFailure { _state.value = DriveUiState.Error(it.message ?: "Failed to load files") }
        }
    }

    fun refresh() = loadFiles(forceRefresh = true)

    fun streamUrl(fileId: String) = repository.streamUrl(fileId)

    class Factory(
        private val repository: DriveRepository,
        private val folderId: String,
        private val folderName: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DriveBrowserViewModel(repository, folderId, folderName) as T
    }
}
