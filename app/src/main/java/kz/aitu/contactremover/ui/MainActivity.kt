package kz.aitu.contactremover.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kz.aitu.contactremover.R
import kz.aitu.contactremover.databinding.ActivityMainBinding
import kz.aitu.contactremover.service.FloatingWindowService
import kz.aitu.contactremover.utils.PermissionHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val CONTACTS_PERMISSION_REQUEST = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatuses()
    }

    private fun setupUI() {
        // Start / Stop service toggle
        binding.btnToggleService.setOnClickListener {
            if (PermissionHelper.hasOverlayPermission(this)) {
                toggleService()
            } else {
                Toast.makeText(
                    this,
                    "Сначала разрешите отображение поверх других приложений",
                    Toast.LENGTH_LONG
                ).show()
                PermissionHelper.requestOverlayPermission(this)
            }
        }

        // Grant overlay permission
        binding.btnGrantOverlay.setOnClickListener {
            PermissionHelper.requestOverlayPermission(this)
        }

        // Enable Accessibility service
        binding.btnGrantAccessibility.setOnClickListener {
            showAccessibilityDialog()
        }

        // Grant contacts permission
        binding.btnGrantContacts.setOnClickListener {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.WRITE_CONTACTS
                ),
                CONTACTS_PERMISSION_REQUEST
            )
        }
    }

    private fun toggleService() {
        val isRunning = isServiceRunning()
        if (isRunning) {
            FloatingWindowService.stop(this)
            binding.btnToggleService.text = "▶  Запустить плавающую кнопку"
            binding.serviceStatusDot.setColorFilter(
                ContextCompat.getColor(this, R.color.status_off)
            )
            binding.tvServiceStatus.text = "Сервис остановлен"
        } else {
            FloatingWindowService.start(this)
            binding.btnToggleService.text = "⏹  Остановить сервис"
            binding.serviceStatusDot.setColorFilter(
                ContextCompat.getColor(this, R.color.status_on)
            )
            binding.tvServiceStatus.text = "Сервис запущен — кнопка активна"
        }
    }

    private fun isServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (FloatingWindowService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun checkPermissions() {
        // Request contacts permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.WRITE_CONTACTS
                ),
                CONTACTS_PERMISSION_REQUEST
            )
        }
    }

    private fun updatePermissionStatuses() {
        // Overlay
        val hasOverlay = PermissionHelper.hasOverlayPermission(this)
        binding.ivOverlayStatus.setImageResource(
            if (hasOverlay) R.drawable.ic_check else R.drawable.ic_warning
        )
        binding.tvOverlayStatus.text =
            if (hasOverlay) "✓ Разрешено" else "✗ Не выдано"
        binding.btnGrantOverlay.isEnabled = !hasOverlay

        // Contacts
        val hasContacts = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        binding.ivContactsStatus.setImageResource(
            if (hasContacts) R.drawable.ic_check else R.drawable.ic_warning
        )
        binding.tvContactsStatus.text =
            if (hasContacts) "✓ Разрешено" else "✗ Не выдано"
        binding.btnGrantContacts.isEnabled = !hasContacts

        // Accessibility
        val accService = "kz.aitu.contactremover/.service.ContactAccessibilityService"
        val hasAccess = PermissionHelper.isAccessibilityEnabled(this, accService)
        binding.ivAccessibilityStatus.setImageResource(
            if (hasAccess) R.drawable.ic_check else R.drawable.ic_warning
        )
        binding.tvAccessibilityStatus.text =
            if (hasAccess) "✓ Включено" else "✗ Не включено (опционально)"
        binding.btnGrantAccessibility.isEnabled = !hasAccess

        // Service running
        val running = isServiceRunning()
        binding.btnToggleService.text =
            if (running) "⏹  Остановить сервис" else "▶  Запустить плавающую кнопку"
        binding.serviceStatusDot.setColorFilter(
            ContextCompat.getColor(
                this,
                if (running) R.color.status_on else R.color.status_off
            )
        )
        binding.tvServiceStatus.text =
            if (running) "Сервис запущен — кнопка активна" else "Сервис остановлен"
    }

    private fun showAccessibilityDialog() {
        AlertDialog.Builder(this, R.style.DarkDialog)
            .setTitle("Служба специальных возможностей")
            .setMessage(
                "Для автоматизации нажатий в WhatsApp Business включите:\n\n" +
                "Настройки → Специальные возможности → Contact Remover\n\n" +
                "Это позволит приложению нажимать кнопки в WhatsApp автоматически."
            )
            .setPositiveButton("Открыть настройки") { _, _ ->
                PermissionHelper.openAccessibilitySettings(this)
            }
            .setNegativeButton("Пропустить", null)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CONTACTS_PERMISSION_REQUEST) {
            updatePermissionStatuses()
        }
    }
}
