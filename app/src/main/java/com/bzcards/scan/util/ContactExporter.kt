package com.bzcards.scan.util

import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import com.bzcards.scan.model.BusinessCard

object ContactExporter {

    private const val PREFS_NAME = "bzcards_prefs"
    private const val KEY_LAST_ACCOUNT_NAME = "last_account_name"
    private const val KEY_LAST_ACCOUNT_TYPE = "last_account_type"

    data class AccountInfo(val name: String, val type: String) {
        val displayName: String get() = name
    }

    fun getGoogleAccounts(context: Context): List<AccountInfo> {
        val accountManager = AccountManager.get(context)
        return accountManager.accounts
            .filter { it.type == "com.google" }
            .map { AccountInfo(it.name, it.type) }
    }

    fun getLastUsedAccount(context: Context): AccountInfo? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_LAST_ACCOUNT_NAME, null) ?: return null
        val type = prefs.getString(KEY_LAST_ACCOUNT_TYPE, null) ?: return null
        return AccountInfo(name, type)
    }

    fun saveLastUsedAccount(context: Context, account: AccountInfo) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_ACCOUNT_NAME, account.name)
            .putString(KEY_LAST_ACCOUNT_TYPE, account.type)
            .apply()
    }

    fun buildContactIntent(card: BusinessCard, account: AccountInfo?): Intent {
        return Intent(ContactsContract.Intents.Insert.ACTION).apply {
            type = ContactsContract.RawContacts.CONTENT_TYPE
            putExtra(ContactsContract.Intents.Insert.NAME, card.name)
            putExtra(ContactsContract.Intents.Insert.PHONE, card.phone)
            putExtra(
                ContactsContract.Intents.Insert.PHONE_TYPE,
                ContactsContract.CommonDataKinds.Phone.TYPE_WORK
            )
            putExtra(ContactsContract.Intents.Insert.EMAIL, card.email)
            putExtra(
                ContactsContract.Intents.Insert.EMAIL_TYPE,
                ContactsContract.CommonDataKinds.Email.TYPE_WORK
            )
            putExtra(ContactsContract.Intents.Insert.COMPANY, card.company)
            putExtra(ContactsContract.Intents.Insert.JOB_TITLE, card.jobTitle)
            putExtra(ContactsContract.Intents.Insert.POSTAL, card.address)
            putExtra(
                ContactsContract.Intents.Insert.POSTAL_TYPE,
                ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK
            )
            if (account != null) {
                putExtra("account_name", account.name)
                putExtra("account_type", account.type)
            }
        }
    }
}
