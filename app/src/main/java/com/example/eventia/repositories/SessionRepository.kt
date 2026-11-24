package com.example.eventia.repositories

import com.example.eventia.api.EventiaApi
import com.example.eventia.api.models.LoginRequestDto
import com.example.eventia.api.models.RegisterRequestDto
import com.example.eventia.data.local.SessionStore
import com.example.eventia.domain.model.Account
import com.google.gson.JsonParser
import java.io.IOException
import javax.inject.Inject
import retrofit2.HttpException
import retrofit2.Response

interface SessionRepository {
    suspend fun hasSession(): Boolean
    suspend fun signIn(email: String, password: String)
    suspend fun register(name: String, email: String, password: String)
    suspend fun loadAccount(): Account?
    suspend fun attendEvent(id: Long)
    suspend fun cancelAttendance(id: Long)
    suspend fun signOut()
}

enum class AccountField { Name, Email, Password }

class AuthenticationRejected(val fields: Set<AccountField> = emptySet()) : Exception()

internal class EventSessionExpired : Exception()

class RemoteSessionRepository @Inject constructor(
    private val api: EventiaApi,
    private val store: SessionStore
) : SessionRepository {
    override suspend fun hasSession(): Boolean = !store.readToken().isNullOrBlank()

    override suspend fun signOut() = store.clearToken()

    override suspend fun signIn(email: String, password: String) = authenticate {
        api.login(LoginRequestDto(email, password)).token
    }

    override suspend fun register(name: String, email: String, password: String) = authenticate {
        api.register(RegisterRequestDto(name, email, password)).token
    }

    override suspend fun attendEvent(id: Long) = updateAttendance(id, api::attendEvent)

    override suspend fun cancelAttendance(id: Long) = updateAttendance(id, api::cancelAttendance)

    private suspend fun updateAttendance(id: Long, request: suspend (Long, String) -> Response<Void>) {
        val token = store.readToken()?.takeIf(String::isNotBlank) ?: throw EventSessionExpired()
        val response = request(id, "Bearer $token")
        if (response.code() == 401) throw EventSessionExpired()
        if (!response.isSuccessful) throw HttpException(response)
    }

    override suspend fun loadAccount(): Account? {
        val token = store.readToken()?.takeIf(String::isNotBlank) ?: return null
        return try {
            val account = api.getMe("Bearer $token")
            val email = account.email?.takeIf(String::isNotBlank)
                ?: throw IOException("Missing account email")
            Account(email, account.name?.takeIf(String::isNotBlank))
        } catch (error: HttpException) {
            if (error.code() != 401) throw error
            store.clearToken()
            null
        }
    }

    private suspend fun authenticate(request: suspend () -> String?) {
        val token = try {
            request()
        } catch (error: HttpException) {
            when (error.code()) {
                400, 422 -> throw AuthenticationRejected(validationFields(error))
                401 -> throw AuthenticationRejected()
                else -> throw error
            }
        }
        if (token.isNullOrBlank()) throw IOException("Missing session token")
        store.saveToken(token)
    }

    private fun validationFields(error: HttpException): Set<AccountField> {
        val fieldNames = runCatching {
            val body = error.response()?.errorBody()?.string() ?: return emptySet()
            JsonParser.parseString(body).asJsonObject.getAsJsonObject("fields").keySet()
        }.getOrNull().orEmpty()
        return buildSet {
            if ("name" in fieldNames) add(AccountField.Name)
            if ("email" in fieldNames) add(AccountField.Email)
            if ("password" in fieldNames) add(AccountField.Password)
        }
    }
}
