package kz.aitu.contactremover.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kz.aitu.contactremover.R
import kz.aitu.contactremover.model.ContactRepository
import kz.aitu.contactremover.model.PhoneContact
import kz.aitu.contactremover.ui.MainActivity
import kz.aitu.contactremover.utils.PermissionHelper
import kotlin.math.abs

class FloatingWindowService : Service() {

    companion object {
        const val CHANNEL_ID = "floating_overlay_channel"
        const val NOTIF_ID = 1001
        
        fun start(context: Context) {
            val intent = Intent(context, FloatingWindowService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingWindowService::class.java))
        }
    }

    private lateinit var windowManager: WindowManager
    
    // Floating button view
    private var fabView: View? = null
    private var panelView: View? = null
    
    // Fab params
    private lateinit var fabParams: WindowManager.LayoutParams
    private lateinit var panelParams: WindowManager.LayoutParams
    
    private var isPanelVisible = false
    private var filteredContacts = ContactRepository.presetContacts.toMutableList()
    private var selectedContact: PhoneContact? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createFloatingButton()
    }

    override fun onDestroy() {
        super.onDestroy()
        fabView?.let { windowManager.removeView(it) }
        panelView?.let { windowManager.removeView(it) }
    }

    // ─── Notification ──────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "WhatsApp Contact Remover overlay service"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val tapIntent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Contact Remover")
            .setContentText("Floating button active — overlay running")
            .setSmallIcon(R.drawable.ic_overlay_notif)
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    // ─── Floating Button ───────────────────────────────────────────────────────

    @SuppressLint("InflateParams")
    private fun createFloatingButton() {
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

        fabParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 400
        }

        fabView = LayoutInflater.from(this).inflate(R.layout.layout_floating_button, null)
        setupFabDrag(fabView!!)

        fabView!!.setOnClickListener {
            if (isPanelVisible) hidePanel() else showPanel()
        }

        windowManager.addView(fabView, fabParams)
    }

    // ─── Dragging ──────────────────────────────────────────────────────────────

    private var touchStartX = 0f
    private var touchStartY = 0f
    private var fabStartX = 0
    private var fabStartY = 0
    private var hasMoved = false

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFabDrag(view: View) {
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    fabStartX = fabParams.x
                    fabStartY = fabParams.y
                    hasMoved = false
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchStartX
                    val dy = event.rawY - touchStartY
                    if (abs(dx) > 8 || abs(dy) > 8) {
                        hasMoved = true
                        fabParams.x = (fabStartX + dx).toInt()
                        fabParams.y = (fabStartY + dy).toInt()
                        windowManager.updateViewLayout(fabView, fabParams)
                        // Move panel along with FAB
                        if (isPanelVisible && panelView != null) {
                            updatePanelPosition()
                            windowManager.updateViewLayout(panelView, panelParams)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> hasMoved
                else -> false
            }
        }
    }

    // ─── Panel ─────────────────────────────────────────────────────────────────

    @SuppressLint("InflateParams")
    private fun showPanel() {
        if (isPanelVisible) return
        isPanelVisible = true

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

        panelParams = WindowManager.LayoutParams(
            (resources.displayMetrics.widthPixels * 0.88f).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        updatePanelPosition()

        panelView = LayoutInflater.from(this).inflate(R.layout.layout_panel, null)
        setupPanel(panelView!!)

        panelView!!.alpha = 0f
        windowManager.addView(panelView, panelParams)
        panelView!!.animate().alpha(1f).setDuration(200).start()
    }

    private fun updatePanelPosition() {
        val display = windowManager.defaultDisplay
        val size = android.graphics.Point()
        @Suppress("DEPRECATION")
        display.getSize(size)
        
        val fabX = fabParams.x
        val fabY = fabParams.y
        val fabSize = (56 * resources.displayMetrics.density).toInt()
        val panelW = (resources.displayMetrics.widthPixels * 0.88f).toInt()
        val margin = (12 * resources.displayMetrics.density).toInt()
        
        panelParams.x = (fabX + fabSize / 2 - panelW / 2)
            .coerceIn(margin, size.x - panelW - margin)
        panelParams.y = fabY + fabSize + margin
        
        if (panelParams.y + 600 > size.y) {
            panelParams.y = fabY - 600 - margin
        }
    }

    private fun hidePanel() {
        if (!isPanelVisible) return
        isPanelVisible = false
        panelView?.animate()?.alpha(0f)?.setDuration(150)?.withEndAction {
            try { windowManager.removeView(panelView) } catch (_: Exception) {}
            panelView = null
        }?.start()
    }

    @SuppressLint("SetTextI18n")
    private fun setupPanel(panel: View) {
        val etPhone = panel.findViewById<EditText>(R.id.etPhone)
        val btnClear = panel.findViewById<ImageButton>(R.id.btnClearInput)
        val etSearch = panel.findViewById<EditText>(R.id.etSearch)
        val listView = panel.findViewById<ListView>(R.id.contactList)
        val btnDelete = panel.findViewById<Button>(R.id.btnDelete)
        val btnClose = panel.findViewById<ImageButton>(R.id.btnClose)
        val tvSelected = panel.findViewById<TextView>(R.id.tvSelected)

        // Close button
        btnClose.setOnClickListener { hidePanel() }

        // Clear phone input
        btnClear.setOnClickListener {
            etPhone.setText("")
            selectedContact = null
            tvSelected.text = ""
            tvSelected.visibility = View.GONE
        }

        // Contact search
        val adapter = ContactListAdapter(this, filteredContacts)
        listView.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val results = ContactRepository.search(s?.toString() ?: "")
                filteredContacts.clear()
                filteredContacts.addAll(results)
                adapter.notifyDataSetChanged()
            }
        })

        listView.setOnItemClickListener { _, _, position, _ ->
            val contact = filteredContacts[position]
            selectedContact = contact
            etPhone.setText(contact.phone)
            tvSelected.text = "✓ ${contact.name} (${contact.carrier})"
            tvSelected.visibility = View.VISIBLE
            // Hide keyboard
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(etPhone.windowToken, 0)
        }

        // Delete button
        btnDelete.setOnClickListener {
            val phone = etPhone.text?.toString()?.trim() ?: ""
            if (phone.isBlank()) {
                Toast.makeText(this, "Введите номер телефона", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            performDelete(phone)
        }
    }

    private fun performDelete(phone: String) {
        val cleaned = PermissionHelper.cleanPhone(phone)
        
        // Try system contact deletion first
        val systemIntent = PermissionHelper.buildSystemDeleteIntent(this, cleaned)
        if (systemIntent != null) {
            hidePanel()
            startActivity(systemIntent)
            return
        }
        
        // Fallback: open contacts app with filter
        val fallbackIntent = Intent(Intent.ACTION_DELETE).apply {
            val uri = android.net.Uri.withAppendedPath(
                android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                android.net.Uri.encode(cleaned)
            )
            data = uri
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        try {
            hidePanel()
            startActivity(fallbackIntent)
        } catch (e: Exception) {
            // Last resort: open contacts and let user search manually
            val contactsIntent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("content://contacts/people")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(contactsIntent)
            Toast.makeText(
                this,
                "Контакт не найден. Введите $cleaned в поиске контактов",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

// ─── Contact List Adapter ──────────────────────────────────────────────────────

class ContactListAdapter(
    context: Context,
    private val contacts: List<PhoneContact>
) : ArrayAdapter<PhoneContact>(context, 0, contacts) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val contact = contacts[position]
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_contact, parent, false)
        
        view.findViewById<TextView>(R.id.tvContactName).text = contact.name
        view.findViewById<TextView>(R.id.tvContactPhone).text = contact.phone
        view.findViewById<TextView>(R.id.tvCarrier).text = contact.carrier
        
        return view
    }
}
