package com.coldcat.browser

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController

class AddonsActivity : AppCompatActivity(), WebExtensionController.PromptDelegate {

    private lateinit var root: View
    private lateinit var progress: ProgressBar
    private lateinit var recommendedContainer: LinearLayout
    private lateinit var installedContainer: LinearLayout
    private var installed: List<WebExtension> = emptyList()
    private var busy = false

    private val controller get() = ColdCatApp.runtime.webExtensionController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_addons)

        root = findViewById(R.id.addonsRoot)
        progress = findViewById(R.id.addonsProgress)
        recommendedContainer = findViewById(R.id.recommendedContainer)
        installedContainer = findViewById(R.id.installedContainer)

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            root.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        findViewById<MaterialToolbar>(R.id.addonsToolbar).setNavigationOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.installFromUrlButton).setOnClickListener { askForUrl() }
        findViewById<MaterialButton>(R.id.browseAmoButton).setOnClickListener { openAmo() }

        controller.setPromptDelegate(this)
        refresh()
    }

    override fun onDestroy() {
        controller.setPromptDelegate(null)
        super.onDestroy()
    }

    private fun refresh() {
        setBusy(true)
        controller.list().accept({ extensions ->
            runOnUiThread {
                installed = extensions.orEmpty().filterNot { it.isBuiltIn }
                setBusy(false)
                renderRecommended()
                renderInstalled()
            }
        }, { error ->
            runOnUiThread {
                setBusy(false)
                toast(getString(R.string.addon_install_failed, error?.message ?: error.toString()))
            }
        })
    }

    private fun renderRecommended() {
        recommendedContainer.removeAllViews()
        AddonCatalog.recommended.forEachIndexed { index, addon ->
            val row = layoutInflater.inflate(R.layout.item_addon, recommendedContainer, false)
            val isInstalled = installed.any {
                it.id == addon.id || it.metaData.name.equals(addon.name, ignoreCase = true)
            }

            row.findViewById<TextView>(R.id.addonIcon).apply {
                text = addon.name.take(1).uppercase()
                backgroundTintList = ColorStateList.valueOf(addon.color)
            }
            row.findViewById<TextView>(R.id.addonName).text = addon.name
            row.findViewById<TextView>(R.id.addonDescription).text = addon.tagline

            val action = row.findViewById<MaterialButton>(R.id.addonAction)
            action.text = getString(if (isInstalled) R.string.installed else R.string.install)
            action.isEnabled = !isInstalled
            action.setOnClickListener { install(addon.url, addon.name) }

            if (index == 0) {
                (row.layoutParams as LinearLayout.LayoutParams).topMargin = 0
            }
            recommendedContainer.addView(row)
        }
    }

    private fun renderInstalled() {
        installedContainer.removeAllViews()
        if (installed.isEmpty()) {
            val empty = TextView(this)
            empty.setText(R.string.addons_none)
            empty.setTextColor(getColor(R.color.text_secondary))
            empty.textSize = 14f
            installedContainer.addView(empty)
            return
        }

        val colors = AddonCatalog.recommended.map { it.color }
        installed.sortedBy { (it.metaData.name ?: it.id).lowercase() }
            .forEachIndexed { index, extension ->
                val name = extension.metaData.name ?: extension.id
                val row = layoutInflater.inflate(R.layout.item_addon, installedContainer, false)
                row.findViewById<TextView>(R.id.addonIcon).apply {
                    text = name.take(1).uppercase()
                    backgroundTintList = ColorStateList.valueOf(colors[index % colors.size])
                }
                row.findViewById<TextView>(R.id.addonName).text = name
                row.findViewById<TextView>(R.id.addonDescription).text =
                    extension.metaData.version + " · " + extension.id
                row.findViewById<MaterialButton>(R.id.addonAction).apply {
                    text = getString(R.string.uninstall)
                    setOnClickListener { confirmRemove(extension) }
                }
                installedContainer.addView(row)
            }
    }

    private fun install(url: String, label: String) {
        if (busy) return
        setBusy(true)
        toast(getString(R.string.addon_installing, label))
        controller.install(url, WebExtensionController.INSTALLATION_METHOD_MANAGER)
            .accept({ extension ->
                runOnUiThread {
                    ColdCatApp.extensionsChanged = true
                    setBusy(false)
                    toast(
                        getString(
                            R.string.addon_installed,
                            extension?.metaData?.name ?: label,
                        ),
                    )
                    refresh()
                }
            }, { error ->
                runOnUiThread {
                    setBusy(false)
                    toast(getString(R.string.addon_install_failed, error?.message ?: error.toString()))
                }
            })
    }

    private fun confirmRemove(extension: WebExtension) {
        val name = extension.metaData.name ?: extension.id
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.uninstall_title, name))
            .setMessage(R.string.uninstall_message)
            .setPositiveButton(R.string.uninstall) { _, _ -> remove(extension) }
            .setNegativeButton(R.string.deny, null)
            .show()
    }

    private fun remove(extension: WebExtension) {
        if (busy) return
        setBusy(true)
        controller.uninstall(extension).accept({
            runOnUiThread {
                ColdCatApp.extensionsChanged = true
                setBusy(false)
                toast(getString(R.string.addon_removed, extension.metaData.name ?: extension.id))
                refresh()
            }
        }, { error ->
            runOnUiThread {
                setBusy(false)
                toast(getString(R.string.addon_remove_failed, error?.message ?: error.toString()))
            }
        })
    }

    private fun askForUrl() {
        val input = EditText(this).apply {
            hint = getString(R.string.install_from_url_hint)
            setText("https://")
            setSelection(length())
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        val container = FrameLayout(this).apply {
            setPadding(dp(24), dp(8), dp(24), 0)
            addView(input)
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.install_url_title)
            .setView(container)
            .setPositiveButton(R.string.install, null)
            .setNegativeButton(R.string.deny, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val url = input.text.toString().trim()
                if (!url.startsWith("https://")) {
                    input.error = getString(R.string.install_from_url_hint)
                    return@setOnClickListener
                }
                dialog.dismiss()
                install(url, Uri.parse(url).host ?: getString(R.string.addons_title))
            }
        }
        dialog.show()
    }

    private fun openAmo() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://addons.mozilla.org/android/")))
        } catch (_: Exception) {
            toast(getString(R.string.open_external_failed))
        }
    }

    private fun setBusy(value: Boolean) {
        busy = value
        progress.visibility = if (value) View.VISIBLE else View.GONE
        findViewById<MaterialButton>(R.id.installFromUrlButton).isEnabled = !value
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onInstallPromptRequest(
        extension: WebExtension,
        permissions: Array<String>,
        origins: Array<String>,
        dataCollectionPermissions: Array<String>,
    ): GeckoResult<WebExtension.PermissionPromptResponse> {
        val result = GeckoResult<WebExtension.PermissionPromptResponse>()
        val name = extension.metaData.name ?: extension.id
        val items = (permissions + origins).filter { it.isNotBlank() }.distinct().take(12)
        val body = if (items.isEmpty()) {
            getString(R.string.permissions_none)
        } else {
            items.joinToString("\n") { "• $it" }
        }

        runOnUiThread {
            if (isFinishing || isDestroyed) {
                result.complete(WebExtension.PermissionPromptResponse(false, false, false))
                return@runOnUiThread
            }
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.permissions_title, name))
                .setMessage(getString(R.string.permissions_message, body))
                .setPositiveButton(R.string.allow) { _, _ ->
                    result.complete(WebExtension.PermissionPromptResponse(true, false, false))
                }
                .setNegativeButton(R.string.deny) { _, _ ->
                    result.complete(WebExtension.PermissionPromptResponse(false, false, false))
                }
                .setOnCancelListener {
                    result.complete(WebExtension.PermissionPromptResponse(false, false, false))
                }
                .show()
        }
        return result
    }

    override fun onUpdatePrompt(
        extension: WebExtension,
        newPermissions: Array<String>,
        newOrigins: Array<String>,
        newDataCollectionPermissions: Array<String>,
    ): GeckoResult<AllowOrDeny> = GeckoResult.allow()

    override fun onOptionalPrompt(
        extension: WebExtension,
        permissions: Array<String>,
        origins: Array<String>,
        dataCollectionPermissions: Array<String>,
    ): GeckoResult<AllowOrDeny> = GeckoResult.allow()
}
