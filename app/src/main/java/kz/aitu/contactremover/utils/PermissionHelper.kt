package kz.aitu.contactremover.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object PermissionHelper {

    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun requestOverlayPermission(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun isAccessibilityEnabled(context: Context, serviceName: String): Boolean {
        val settingValue = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return settingValue.contains(serviceName)
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Builds a WhatsApp Business delete contact URI.
     * wa.me scheme opens WhatsApp with phone number, ACTION_DELETE removes from contacts.
     */
    fun buildDeleteContactIntent(phone: String): Intent {
        val cleaned = cleanPhone(phone)
        val uri = Uri.withAppendedPath(
            android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(cleaned)
        )
        return Intent(Intent.ACTION_DELETE, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Opens WhatsApp Business chat for a phone number (wa.me link).
     */
    fun buildWhatsAppIntent(phone: String): Intent {
        val cleaned = cleanPhone(phone).removePrefix("+")
        val uri = Uri.parse("https://wa.me/$cleaned")
        return Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp.w4b") // WhatsApp Business
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // Fallback to regular WhatsApp
        }
    }

    /**
     * Opens system contact deletion dialog by looking up phone number.
     */
    fun buildSystemDeleteIntent(context: Context, phone: String): Intent? {
        val cleaned = cleanPhone(phone)
        val cursor = context.contentResolver.query(
            android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon()
                .appendPath(cleaned).build(),
            arrayOf(
                android.provider.ContactsContract.PhoneLookup.LOOKUP_KEY,
                android.provider.ContactsContract.PhoneLookup._ID
            ),
            null, null, null
        ) ?: return null

        return cursor.use {
            if (it.moveToFirst()) {
                val lookupKey = it.getString(0)
                val contactId = it.getLong(1)
                val contactUri = android.provider.ContactsContract.Contacts.getLookupUri(
                    contactId, lookupKey
                )
                Intent(Intent.ACTION_DELETE, contactUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else null
        }
    }

    fun cleanPhone(phone: String): String {
        return phone.replace(Regex("[\\s\\-\\(\\)]"), "")
    }
}
