package kz.aitu.contactremover.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Accessibility Service for automating WhatsApp Business contact deletion.
 * Listens to window state changes and can trigger UI interactions.
 */
class ContactAccessibilityService : AccessibilityService() {

    companion object {
        const val ACTION_CLICK_DELETE = "kz.aitu.contactremover.ACTION_CLICK_DELETE"
        const val EXTRA_PHONE = "extra_phone"
        
        var instance: ContactAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Detect when WhatsApp Business is in foreground
        val pkgName = event?.packageName?.toString() ?: return
        if (pkgName == "com.whatsapp.w4b" || pkgName == "com.whatsapp") {
            // Could be extended to detect chat screen and auto-act
        }
    }

    override fun onInterrupt() {
        instance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    /**
     * Finds and clicks a button by text anywhere in the current window.
     */
    fun clickButtonByText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(text)
        for (node in nodes) {
            if (node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
            // Try parent
            val parent = node.parent
            if (parent?.isClickable == true) {
                parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
        }
        return false
    }
}
