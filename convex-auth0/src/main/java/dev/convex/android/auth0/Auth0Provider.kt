package dev.convex.android.auth0

import android.content.Context
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.authentication.storage.CredentialsManagerException
import com.auth0.android.authentication.storage.SecureCredentialsManager
import com.auth0.android.authentication.storage.SharedPreferencesStorage
import com.auth0.android.authentication.storage.Storage
import com.auth0.android.callback.Callback
import com.auth0.android.jwt.JWT
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import dev.convex.android.AuthProvider
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val SCOPES = "openid profile email offline_access"

/**
 * Enables a `ConvexClientWithAuth` to use [Auth0](https://auth0.com/).
 *
 * Successful logins provide Auth0's [Credentials] for the authenticated user. They can be used to
 * customize the app experience for your users.
 *
 * @param context an Android [Context]
 * @param clientId the Auth0 client ID configured for your application
 * @param domain the Auth0 domain configured for your application
 * @param scheme the scheme used in your Auth0 login and logout callback URLs
 * @param enableCachedLogins whether to use Auth0's [SecureCredentialsManager] for caching credentials
 * @param credentialsStorage optional [Storage] implementation for credentials; defaults to [SharedPreferencesStorage]
 */
class Auth0Provider(
    context: Context,
    clientId: String,
    domain: String,
    private val scheme: String = "app",
    enableCachedLogins: Boolean = false,
    credentialsStorage: Storage? = null
) : AuthProvider<Credentials> {
    private val auth0 = Auth0(clientId, domain)
    private val credentialsManager: SecureCredentialsManager? =
        if (enableCachedLogins) SecureCredentialsManager(
            context,
            AuthenticationAPIClient(auth0),
            credentialsStorage ?: SharedPreferencesStorage(context)
        ) else null

    override suspend fun login(context: Context): Result<Credentials> = suspendCoroutine { cont ->
        WebAuthProvider.login(auth0).withScheme(scheme)
            .withScope(SCOPES)
            .start(context, object : Callback<Credentials, AuthenticationException> {
                override fun onFailure(error: AuthenticationException) {
                    cont.resume(Result.failure(error))
                }

                override fun onSuccess(result: Credentials) {
                    credentialsManager?.saveCredentials(result)
                    cont.resume(Result.success(result))
                }
            })
    }

    override suspend fun loginFromCache(): Result<Credentials> {
        if (credentialsManager == null) {
            return Result.failure(CachedLoginsNotEnabledError())
        }
        try {
            var credentials = credentialsManager.awaitCredentials()
            // Convex applications share the ID token with the backend so both the client app and
            // backend can share the authenticated state. Auth0 only considers credentials expired
            // when the access token expires. Convex considers them expired when the ID token
            // expires. To ensure that we're only passing unexpired ID tokens to the backend, we
            // check the ID token expiration and force a token refresh if it is expired. Here are
            // a couple of links to reference:
            // * https://docs.convex.dev/auth/auth0#under-the-hood
            // * https://github.com/auth0/Auth0.Android/pull/572
            val idToken = JWT(credentials.idToken)
            if (idToken.isExpired(0)) {
                credentials = credentialsManager.awaitCredentials(
                    scope = SCOPES,
                    minTtl = 0,
                    parameters = emptyMap(),
                    forceRefresh = true
                )
            }
            return Result.success(credentials)
        } catch (e: CredentialsManagerException) {
            return Result.failure(e)
        }
    }

    override suspend fun logout(context: Context): Result<Void?> = suspendCoroutine { cont ->
        WebAuthProvider.logout(auth0).withScheme(scheme)
            .start(context, object : Callback<Void?, AuthenticationException> {
                override fun onFailure(error: AuthenticationException) {
                    cont.resume(Result.failure(error))
                }

                override fun onSuccess(result: Void?) {
                    cont.resume(Result.success(result))
                }
            })
    }

    override fun extractIdToken(authResult: Credentials): String = authResult.idToken
}

/**
 * A developer error thrown when attempting to use [AuthProvider.loginFromCache] when logging in
 * using cached credentials hasn't been enabled.
 */
class CachedLoginsNotEnabledError : Exception()