package org.who.ddccverifier

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.who.ddccverifier.services.AuthStateManager
import android.content.Intent
import android.net.Uri

import org.who.ddccverifier.services.ConnectionBuilderForTesting
import net.openid.appauth.*
import net.openid.appauth.EndSessionRequest

import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.browser.AnyBrowserMatcher
import net.openid.appauth.AuthState




open class AuthActivity : AppCompatActivity() {
    private lateinit var authState: AuthStateManager

    private val RC_AUTH = 100
    private val RC_END_SESSION = 101

    private val CLIENT_ID = BuildConfig.OPENID_CLIENT_ID
    private val mRedirectURI = Uri.parse(BuildConfig.OPENID_REDIRECT_URI)
    private val mDiscoveryURI = Uri.parse(BuildConfig.OPENID_SERVER_URL)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authState = AuthStateManager.getInstance(this)
        init()
    }

    private fun init() = runBlocking {
        val uiScope = CoroutineScope(Dispatchers.Main + Job())

        uiScope.launch {
            withContext(Dispatchers.IO) {
                backgroundInit()
            }
        }
    }

    fun updateAccountState() {
        onAccountState(authState.current.isAuthorized)
    }
    
    open fun onAccountState(isAuthorized: Boolean) {}

    open fun backgroundInit() {
        AuthorizationServiceConfiguration.fetchFromIssuer(mDiscoveryURI, { config, ex ->
            config?.let {
                authState.replace(AuthState(it))
            }
            ex?.let { ex.printStackTrace() }
        }, ConnectionBuilderForTesting)
    }

    fun requestAuthorization() {
        val authRequest = AuthorizationRequest.Builder(
            authState.current.authorizationServiceConfiguration!!,
            CLIENT_ID,
            ResponseTypeValues.CODE,
            mRedirectURI).build()

        val authService = newAuthService()
        val intentBuilder = authService.createCustomTabsIntentBuilder(authRequest.toUri())
        val authIntent = authService.getAuthorizationRequestIntent(authRequest, intentBuilder.build())
        startActivityForResult(authIntent, RC_AUTH)
    }

    private fun newAuthService(): AuthorizationService {
        val builder = AppAuthConfiguration.Builder()
        //builder.setBrowserMatcher(AnyBrowserMatcher.INSTANCE)
        builder.setConnectionBuilder(ConnectionBuilderForTesting)

        return AuthorizationService(this, builder.build())
    }

    fun requestSignOff() {
        val endSessionRequest = EndSessionRequest.Builder(
            authState.current.authorizationServiceConfiguration!!)
            .setIdTokenHint(authState.current.idToken)
            .setPostLogoutRedirectUri(mRedirectURI)
            .build()

        val endSessionItent = newAuthService().getEndSessionRequestIntent(endSessionRequest)
        startActivityForResult(endSessionItent, RC_END_SESSION)
    }

    fun requestToken(request: TokenRequest) {
        val authService = newAuthService()
        authService.performTokenRequest(request) { resp, ex ->
            authState.updateAfterTokenResponse(resp, ex)
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
                authState.updateAfterAuthorization(it, AuthorizationException.fromIntent(data))
                requestTokenAsync(it.createTokenExchangeRequest())
            }
        } else if (requestCode == RC_END_SESSION) {
            // discard the authorization and token state, but retain the configuration and
            // dynamic client registration (if applicable), to save from retrieving them again.
            val clearedState = AuthState(authState.current.authorizationServiceConfiguration!!)
            if (authState.current.lastRegistrationResponse != null) {
                clearedState.update(authState.current.lastRegistrationResponse)
            }
            authState.replace(clearedState)
        }

        updateAccountState()
    }
}