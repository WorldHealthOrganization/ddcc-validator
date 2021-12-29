package org.who.ddccverifier

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.who.ddccverifier.services.AuthStateManager
import android.content.Intent
import android.net.Uri
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.openid.appauth.*

import org.who.ddccverifier.services.ConnectionBuilderForTesting

import java.net.URL
import java.util.concurrent.atomic.AtomicReference


abstract class AuthActivity : AppCompatActivity() {
    private lateinit var mAuthStateManager: AuthStateManager
    private lateinit var mAuthService: AuthorizationService

    private val mUserInfo: AtomicReference<User> = AtomicReference()

    private val RC_AUTH = 100
    private val RC_END_SESSION = 101
    private val KEY_USER_INFO = "userInfo"

    private val CLIENT_ID = BuildConfig.OPENID_CLIENT_ID
    private val mRedirectURI = Uri.parse(BuildConfig.OPENID_REDIRECT_URI)
    private val mDiscoveryURI = Uri.parse(BuildConfig.OPENID_SERVER_URL)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAuthStateManager = AuthStateManager.getInstance(this)

        val builder = AppAuthConfiguration.Builder()
        builder.setConnectionBuilder(ConnectionBuilderForTesting)
        mAuthService = AuthorizationService(this, builder.build())

        init()

        val userInfo = savedInstanceState?.getString(KEY_USER_INFO)
        if (userInfo != null) {
            mUserInfo.set(jacksonObjectMapper().readValue(savedInstanceState.getString(KEY_USER_INFO), User::class.java))
        }
    }

    override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)

        if (mUserInfo.get() != null) {
            state.putString(KEY_USER_INFO, jacksonObjectMapper().writeValueAsString(mUserInfo.get()))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mAuthService.dispose()
    }

    private fun init() {
        val uiScope = CoroutineScope(Dispatchers.Main + Job())
        uiScope.launch(Dispatchers.IO) {
            backgroundInit()
        }
    }

    fun updateAccountState() = runOnUiThread {
        onAccountState(
            mAuthStateManager.current.authorizationServiceConfiguration?.discoveryDoc != null,
            mAuthStateManager.current.isAuthorized)
    }

    fun updateNewUserInfo() = runOnUiThread {
        onNewUserInfo(mUserInfo.get())
    }

    abstract fun onAccountState(isReady: Boolean, isAuthorized: Boolean)
    abstract fun onNewUserInfo(userInfo: User)

    open fun backgroundInit() {
        fetchAuthConfig()
    }

    fun fetchAuthConfig() {
        AuthorizationServiceConfiguration.fetchFromIssuer(
            mDiscoveryURI,
            this::fetchAuthConfigCallback,
            ConnectionBuilderForTesting)
    }

    fun fetchAuthConfigCallback(config: AuthorizationServiceConfiguration?, ex: AuthorizationException?) {
        config?.let {
            mAuthStateManager.replace(AuthState(it))
            updateAccountState()
        }
        ex?.printStackTrace()
    }

    fun requestAuthorization() {
        val authRequest = AuthorizationRequest.Builder(
            mAuthStateManager.current.authorizationServiceConfiguration!!,
            CLIENT_ID,
            ResponseTypeValues.CODE,
            mRedirectURI).build()

        val intentBuilder = mAuthService.createCustomTabsIntentBuilder(authRequest.toUri())
        val authIntent = mAuthService.getAuthorizationRequestIntent(authRequest, intentBuilder.build())
        startActivityForResult(authIntent, RC_AUTH)
    }

    fun requestSignOff() {
        val endSessionRequest = EndSessionRequest.Builder(
            mAuthStateManager.current.authorizationServiceConfiguration!!)
            .setIdTokenHint(mAuthStateManager.current.idToken)
            .setPostLogoutRedirectUri(mRedirectURI)
            .build()

        val endSessionItent = mAuthService.getEndSessionRequestIntent(endSessionRequest)
        startActivityForResult(endSessionItent, RC_END_SESSION)
    }

    fun requestToken(request: TokenRequest) {
        mAuthService.performTokenRequest(request) { resp, ex ->
            mAuthStateManager.updateAfterTokenResponse(resp, ex)
            mAuthService.performTokenRequest(mAuthStateManager.current.createTokenRefreshRequest()) { _, _ ->
                fetchUserInfo()
            }
            updateAccountState()
        }
    }

    private fun requestTokenAsync(request: TokenRequest) = runBlocking {
        val uiScope = CoroutineScope(Dispatchers.Main + Job())

        uiScope.launch {
            withContext(Dispatchers.IO) {
                requestToken(request)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_CANCELED) {
            // Canceled log in
        } else if (requestCode == RC_AUTH && data != null) {
            AuthorizationResponse.fromIntent(data)?.let {
                mAuthStateManager.updateAfterAuthorization(it, AuthorizationException.fromIntent(data))
                requestTokenAsync(it.createTokenExchangeRequest())
            }
        } else if (requestCode == RC_END_SESSION) {
            // discard the authorization and token state, but retain the configuration and
            // dynamic client registration (if applicable), to save from retrieving them again.
            val clearedState = AuthState(mAuthStateManager.current.authorizationServiceConfiguration!!)
            if (mAuthStateManager.current.lastRegistrationResponse != null) {
                clearedState.update(mAuthStateManager.current.lastRegistrationResponse)
            }
            mAuthStateManager.replace(clearedState)
            mUserInfo.set(null)
        }

        updateAccountState()
    }

    /**
     * User Info
     */
    data class User(
        val sub: String,
        val name: String?,
        val preferredUsername: String?,
        val givenName: String?,
        val familyName: String?,
        val email: String?,
        val emailVerified: Boolean?,
    )

    fun fetchUserInfo() {
        mAuthStateManager.current.performActionWithFreshTokens(mAuthService, this::fetchUserInfoCallback)
    }

    fun fetchUserInfoCallback(accessToken: String?, idToken: String?, ex: AuthorizationException?) {
        if (ex != null) {
            ex.printStackTrace()
            return
        }

        val uiScope = CoroutineScope(Dispatchers.Main + Job())
        uiScope.launch {
            withContext(Dispatchers.IO) {
                val uri = mAuthStateManager.current.authorizationServiceConfiguration?.discoveryDoc?.userinfoEndpoint

                val conn = URL(uri.toString()).openConnection()
                conn.setRequestProperty("Authorization", "Bearer $accessToken")

                val mapper = jacksonObjectMapper()
                mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                mUserInfo.set(mapper.readValue(conn.getInputStream(), User::class.java))
                updateNewUserInfo()
            }
        }
    }
}