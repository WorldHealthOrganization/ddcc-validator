package org.who.ddccverifier.services

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import net.openid.appauth.*
import org.json.JSONException
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock

/**
 * An example persistence mechanism for an [AuthState] instance.
 * This stores the instance in a shared preferences file, and provides thread-safe access and
 * mutation.
 */
class AuthStateManager private constructor(context: Context) {
    private val mPrefs: SharedPreferences
    private val mPrefsLock: ReentrantLock
    private val mCurrentAuthState: AtomicReference<AuthState>

    val current: AuthState
        get() {
            if (mCurrentAuthState.get() != null) {
                return mCurrentAuthState.get()
            }
            val state = readState()
            return if (mCurrentAuthState.compareAndSet(null, state)) {
                state
            } else {
                mCurrentAuthState.get()
            }
        }

    fun replace(state: AuthState): AuthState {
        writeState(state)
        mCurrentAuthState.set(state)
        return state
    }

    fun updateAfterAuthorization(response: AuthorizationResponse?, ex: AuthorizationException?): AuthState {
        val current = current
        current.update(response, ex)
        return replace(current)
    }

    fun updateAfterTokenResponse(response: TokenResponse?, ex: AuthorizationException?): AuthState {
        val current = current
        current.update(response, ex)
        return replace(current)
    }

    fun updateAfterRegistration(response: RegistrationResponse?, ex: AuthorizationException?): AuthState {
        val current = current
        if (ex != null) {
            return current
        }
        current.update(response)
        return replace(current)
    }

    private fun readState(): AuthState {
        mPrefsLock.lock()
        return try {
            val currentState = mPrefs.getString(KEY_STATE, null)
                ?: return AuthState()

            try {
                AuthState.jsonDeserialize(currentState)
            } catch (ex: JSONException) {
                Log.w(TAG, "Failed to deserialize stored auth state - discarding")
                AuthState()
            }
        } finally {
            mPrefsLock.unlock()
        }
    }

    private fun writeState(state: AuthState?) {
        mPrefsLock.lock()
        try {
            val editor = mPrefs.edit()
            if (state == null) {
                editor.remove(KEY_STATE)
            } else {
                editor.putString(KEY_STATE, state.jsonSerializeString())
            }
            check(editor.commit()) { "Failed to write state to shared prefs" }
        } finally {
            mPrefsLock.unlock()
        }
    }

    companion object {
        private val INSTANCE_REF = AtomicReference(WeakReference<AuthStateManager?>(null))
        private const val TAG = "AuthStateManager"
        private const val STORE_NAME = "AuthState"
        private const val KEY_STATE = "state"

        fun getInstance(context: Context): AuthStateManager {
            var manager = INSTANCE_REF.get().get()
            if (manager == null) {
                manager = AuthStateManager(context.applicationContext)
                INSTANCE_REF.set(WeakReference(manager))
            }
            return manager
        }
    }

    init {
        mPrefs = context.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE)
        mPrefsLock = ReentrantLock()
        mCurrentAuthState = AtomicReference()
    }
}