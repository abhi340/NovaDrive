package com.example.tvdrive

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tvdrive.auth.AuthManager
import com.example.tvdrive.auth.AuthState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Central ViewModel — owns the navigation backstack and auth state.
 * Lives for the entire lifetime of MainActivity (survives rotation).
 *
 * Navigation model: List<Screen> where last() = current screen.
 * Previous screens are NOT retained in Compose — only the current screen
 * is composed, keeping peak RAM low.
 */
class AppViewModel(val authManager: AuthManager) : ViewModel() {

    private val _backStack = kotlinx.coroutines.flow.MutableStateFlow<List<Screen>>(emptyList())
    val backStack: StateFlow<List<Screen>> = _backStack

    val currentScreen: StateFlow<Screen?> = _backStack
        .map { it.lastOrNull() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val authState = authManager.authState

    init {
        // Initialise backstack based on auth state
        viewModelScope.launch {
            authManager.authState.collect { state ->
                when (state) {
                    is AuthState.Loading -> { /* wait for resolution */ }
                    is AuthState.SignedOut, is AuthState.Error -> {
                        _backStack.value = listOf(Screen.SignIn)
                    }
                    is AuthState.SignedIn -> {
                        // Only reset to Home if we're on the SignIn screen
                        if (_backStack.value.isEmpty() || _backStack.value.last() == Screen.SignIn) {
                            _backStack.value = listOf(Screen.Home())
                        }
                    }
                }
            }
        }
    }

    /** Push a new screen onto the backstack */
    fun navigate(screen: Screen) {
        _backStack.value = _backStack.value + screen
    }

    /** Pop current screen. Returns false if already at root (no more back) */
    fun back(): Boolean {
        if (_backStack.value.size <= 1) return false
        _backStack.value = _backStack.value.dropLast(1)
        return true
    }

    /** Reset stack to a single root screen (e.g., after sign-out) */
    fun navigateRoot(screen: Screen) {
        _backStack.value = listOf(screen)
    }

    /** Replace the current screen without adding to backstack (e.g., tab switch) */
    fun replace(screen: Screen) {
        val stack = _backStack.value.toMutableList()
        if (stack.isNotEmpty()) stack[stack.lastIndex] = screen
        else stack.add(screen)
        _backStack.value = stack
    }

    fun handleSignInResult(data: Intent?) {
        viewModelScope.launch { authManager.handleSignInResult(data) }
    }

    fun signOut() {
        viewModelScope.launch { authManager.signOut() }
    }

    class Factory(private val authManager: AuthManager) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AppViewModel(authManager) as T
    }
}
