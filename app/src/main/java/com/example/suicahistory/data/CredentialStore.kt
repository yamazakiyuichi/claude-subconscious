package com.example.suicahistory.data

import android.content.Context

object CredentialStore {
    private const val PREF = "suica_creds"
    private const val KEY_EMAIL = "email"
    private const val KEY_PASS = "password"

    fun save(context: Context, email: String, password: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString(KEY_EMAIL, email)
            .putString(KEY_PASS, password)
            .apply()
    }

    fun load(context: Context): Pair<String, String>? {
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val email = p.getString(KEY_EMAIL, null) ?: return null
        val pass = p.getString(KEY_PASS, null) ?: return null
        return email to pass
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun hasSaved(context: Context) = load(context) != null
}
