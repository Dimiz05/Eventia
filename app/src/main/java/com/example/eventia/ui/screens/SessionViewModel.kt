package com.example.eventia.ui.screens

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventia.R
import com.example.eventia.domain.model.Account
import com.example.eventia.repositories.AccountField
import com.example.eventia.repositories.AuthenticationRejected
import com.example.eventia.repositories.SessionRepository
import com.google.gson.JsonParseException
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed interface SessionUiState {
    data object Starting : SessionUiState
    data object StartupError : SessionUiState
    data object Guest : SessionUiState
    data class Form(
        val registering: Boolean = false,
        val submitting: Boolean = false,
        val errors: Map<AccountField, Int> = emptyMap(),
        val error: Int? = null
    ) : SessionUiState

    data class Active(
        val account: Account? = null,
        val loading: Boolean = true,
        val signingOut: Boolean = false,
        val signOutFailed: Boolean = false
    ) : SessionUiState
}

@HiltViewModel
class SessionViewModel @Inject constructor(private val repository: SessionRepository) : ViewModel() {
    private val mutableState = MutableStateFlow<SessionUiState>(SessionUiState.Starting)
    val state = mutableState.asStateFlow()
    private var sessionRequest: Job? = null

    init {
        restore()
    }

    fun retryStartup() {
        if (mutableState.value == SessionUiState.StartupError) restore()
    }

    private fun restore() {
        mutableState.value = SessionUiState.Starting
        sessionRequest = viewModelScope.launch {
            try {
                if (repository.hasSession()) loadAccount() else mutableState.value = SessionUiState.Form()
            } catch (_: IOException) {
                mutableState.value = SessionUiState.StartupError
            }
        }
    }

    fun continueAsGuest() {
        val form = mutableState.value as? SessionUiState.Form ?: return
        if (!form.submitting) mutableState.value = SessionUiState.Guest
    }

    fun openSignIn() {
        if (mutableState.value == SessionUiState.Guest) mutableState.value = SessionUiState.Form()
    }

    fun signOut() {
        val current = mutableState.value as? SessionUiState.Active ?: return
        if (current.signingOut) return
        mutableState.value = current.copy(signingOut = true, signOutFailed = false)
        val pendingRequest = sessionRequest
        sessionRequest = viewModelScope.launch {
            pendingRequest?.cancelAndJoin()
            try {
                repository.signOut()
                mutableState.value = SessionUiState.Form()
            } catch (_: IOException) {
                mutableState.value = current.copy(loading = false, signOutFailed = true)
            }
        }
    }

    fun switchForm() {
        val form = mutableState.value as? SessionUiState.Form ?: return
        if (!form.submitting) mutableState.value = SessionUiState.Form(registering = !form.registering)
    }

    fun clearError(field: AccountField) {
        val form = mutableState.value as? SessionUiState.Form ?: return
        if (!form.submitting) mutableState.value = form.copy(errors = form.errors - field, error = null)
    }

    fun submit(name: String, email: String, password: String) {
        val form = mutableState.value as? SessionUiState.Form ?: return
        if (form.submitting) return
        val cleanEmail = email.trim()
        val errors = buildMap {
            if (form.registering && name.isBlank()) put(AccountField.Name, R.string.auth_name_required)
            if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                put(AccountField.Email, R.string.auth_invalid_email)
            }
            if (password.length < 8) put(AccountField.Password, R.string.auth_short_password)
        }
        if (errors.isNotEmpty()) {
            mutableState.value = form.copy(errors = errors, error = null)
            return
        }
        val submitting = form.copy(submitting = true, errors = emptyMap(), error = null)
        mutableState.value = submitting
        sessionRequest = viewModelScope.launch {
            try {
                if (form.registering) repository.register(name.trim(), cleanEmail, password)
                else repository.signIn(cleanEmail, password)
                loadAccount()
            } catch (error: AuthenticationRejected) {
                val rejected = error.fields.filter { form.registering || it != AccountField.Name }
                mutableState.value = submitting.copy(
                    submitting = false,
                    errors = rejected.associateWith { field ->
                        if (!form.registering && field == AccountField.Email) {
                            R.string.auth_credentials_rejected
                        } else R.string.auth_field_rejected
                    },
                    error = if (rejected.isEmpty()) {
                        if (form.registering) R.string.auth_registration_rejected
                        else R.string.auth_credentials_rejected
                    } else null
                )
            } catch (_: IOException) {
                submitFailed(submitting)
            } catch (_: HttpException) {
                submitFailed(submitting)
            } catch (_: JsonParseException) {
                submitFailed(submitting)
            }
        }
    }

    private fun submitFailed(form: SessionUiState.Form) {
        mutableState.value = form.copy(submitting = false, error = R.string.auth_submit_error)
    }

    fun retryAccount() {
        val current = mutableState.value as? SessionUiState.Active ?: return
        if (current.loading || current.signingOut) return
        mutableState.value = current.copy(loading = true)
        sessionRequest = viewModelScope.launch { loadAccount() }
    }

    private suspend fun loadAccount() {
        mutableState.value = SessionUiState.Active()
        try {
            val account = repository.loadAccount()
            mutableState.value = if (account == null) SessionUiState.Form(error = R.string.auth_expired)
            else SessionUiState.Active(account, loading = false)
        } catch (_: IOException) {
            accountLoadFailed()
        } catch (_: HttpException) {
            accountLoadFailed()
        } catch (_: JsonParseException) {
            accountLoadFailed()
        }
    }

    private fun accountLoadFailed() {
        mutableState.value = SessionUiState.Active(loading = false)
    }
}
