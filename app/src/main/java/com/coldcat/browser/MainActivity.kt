package com.coldcat.browser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView

class MainActivity : AppCompatActivity() {

    private lateinit var root: View
    private lateinit var topBar: View
    private lateinit var bottomBar: View
    private lateinit var geckoView: GeckoView
    private lateinit var urlBar: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var reloadButton: ImageButton
    private lateinit var backButton: ImageButton
    private lateinit var forwardButton: ImageButton
    private lateinit var tabBadge: TextView

    private val tabs = mutableListOf<Tab>()
    private var currentIndex = -1
    private val currentTab: Tab? get() = tabs.getOrNull(currentIndex)

    private val navigationDelegate = object : GeckoSession.NavigationDelegate {
        override fun onLocationChange(
            session: GeckoSession,
            url: String?,
            perms: List<GeckoSession.PermissionDelegate.ContentPermission>,
            hasUserGesture: Boolean,
        ) {
            val tab = findTab(session) ?: return
            tab.url = url ?: return
            if (session == currentTab?.session && !urlBar.hasFocus()) {
                urlBar.setText(if (UrlUtils.isHome(url)) "" else url)
            }
        }

        override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
            findTab(session)?.canGoBack = canGoBack
            if (session == currentTab?.session) updateNavigation()
        }

        override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
            findTab(session)?.canGoForward = canGoForward
            if (session == currentTab?.session) updateNavigation()
        }

        override fun onLoadRequest(
            session: GeckoSession,
            request: GeckoSession.NavigationDelegate.LoadRequest,
        ): GeckoResult<AllowOrDeny>? {
            val uri = Uri.parse(request.uri)
            val scheme = uri.scheme?.lowercase() ?: return null

            // Search form on the built-in home page: use the selected engine.
            if (scheme == "coldcat") {
                val query = uri.getQueryParameter("q")
                if (!query.isNullOrBlank()) {
                    val target = ColdCatApp.searchEngine(this@MainActivity).searchUrl(query)
                    CrashLog.checkpoint(this@MainActivity, "home:search")
                    session.loadUri(target)
                }
                return GeckoResult.deny()
            }

            if (scheme in INTERNAL_SCHEMES) return null

            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(request.uri)))
            } catch (_: Exception) {
                Toast.makeText(this@MainActivity, R.string.open_external_failed, Toast.LENGTH_SHORT).show()
            }
            return GeckoResult.deny()
        }
    }

    private val progressDelegate = object : GeckoSession.ProgressDelegate {
        override fun onPageStart(session: GeckoSession, url: String) {
            val tab = findTab(session) ?: return
            tab.loading = true
            if (session == currentTab?.session) {
                CrashLog.checkpoint(this@MainActivity, "page:start ${UrlUtils.pretty(url)}")
                progressBar.visibility = View.VISIBLE
                progressBar.progress = 0
                reloadButton.setImageResource(R.drawable.ic_close)
                reloadButton.contentDescription = getString(R.string.stop)
            }
        }

        override fun onPageStop(session: GeckoSession, success: Boolean) {
            val tab = findTab(session) ?: return
            tab.loading = false
            if (session == currentTab?.session) {
                CrashLog.checkpoint(this@MainActivity, "page:stop success=$success")
                progressBar.visibility = View.INVISIBLE
                reloadButton.setImageResource(R.drawable.ic_reload)
                reloadButton.contentDescription = getString(R.string.reload)
            }
        }

        override fun onProgressChange(session: GeckoSession, progress: Int) {
            if (session != currentTab?.session) return
            if (progress in 1..99) {
                progressBar.visibility = View.VISIBLE
                progressBar.progress = progress
            } else if (progress >= 100) {
                progressBar.visibility = View.INVISIBLE
            }
        }
    }

    private val contentDelegate = object : GeckoSession.ContentDelegate {
        override fun onTitleChange(session: GeckoSession, title: String?) {
            findTab(session)?.title = title.orEmpty()
        }

        override fun onCrash(session: GeckoSession) {
            val tab = findTab(session) ?: return
            session.open(ColdCatApp.runtime)
            session.loadUri(tab.url)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashLog.checkpoint(this, "activity:onCreate")
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        root = findViewById(R.id.root)
        topBar = findViewById(R.id.topBar)
        bottomBar = findViewById(R.id.bottomBar)
        geckoView = findViewById(R.id.geckoView)
        urlBar = findViewById(R.id.urlBar)
        progressBar = findViewById(R.id.progressBar)
        reloadButton = findViewById(R.id.reloadButton)
        backButton = findViewById(R.id.backButton)
        forwardButton = findViewById(R.id.forwardButton)
        tabBadge = findViewById(R.id.tabBadge)

        applyInsets()
        WindowInsetsControllerCompat(window, root).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        urlBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                load(urlBar.text.toString())
                true
            } else {
                false
            }
        }
        reloadButton.setOnClickListener { toggleLoading() }
        backButton.setOnClickListener { currentTab?.session?.goBack() }
        forwardButton.setOnClickListener { currentTab?.session?.goForward() }
        findViewById<ImageButton>(R.id.homeButton).setOnClickListener {
            currentTab?.session?.loadUri(UrlUtils.HOME)
            urlBar.setText("")
        }
        findViewById<ImageButton>(R.id.tabsButton).setOnClickListener { showTabs() }
        findViewById<ImageButton>(R.id.menuButton).setOnClickListener { showMenu() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentTab?.canGoBack == true) {
                    currentTab?.session?.goBack()
                } else {
                    finish()
                }
            }
        })

        if (ColdCatApp.startupError == null) {
            if (savedInstanceState != null) restoreTabs(savedInstanceState) else startFirstTab()
        }
        updateNavigation()
        updateBadge()
        CrashLog.checkpoint(this, "activity:onCreate done")

        ColdCatApp.startupError?.let { error ->
            showDiagnostics(
                getString(R.string.startup_failed),
                CrashLog.describe(this, error, "startup"),
            )
        } ?: CrashLog.read(this)?.let { report ->
            CrashLog.clear(this)
            ExitInfo.pending(this)?.let { ExitInfo.markShown(this, it) }
            showDiagnostics(getString(R.string.crash_title), report)
        } ?: ExitInfo.pending(this)?.let { report ->
            ExitInfo.markShown(this, report)
            showDiagnostics(getString(R.string.exit_report_title), report.render(this))
        } ?: uncleanExitFallback()
    }

    /**
     * Before API 30 there is no exit history. Only there do we guess from the
     * session marker; on newer versions swiping the app away would look like a
     * crash even though it was a normal user action.
     */
    private fun uncleanExitFallback() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) return
        CrashLog.previousUncleanExit ?: return
        showDiagnostics(
            getString(R.string.crash_title),
            CrashLog.describeUncleanExit(this),
        )
    }

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            topBar.updatePadding(
                left = dp(8),
                top = bars.top + dp(8),
                right = dp(8),
                bottom = dp(8),
            )
            bottomBar.updatePadding(
                left = dp(4),
                top = dp(4),
                right = dp(4),
                bottom = maxOf(bars.bottom, ime.bottom) + dp(4),
            )
            insets
        }
    }

    private fun startFirstTab() {
        val start = intentUrl(intent) ?: UrlUtils.HOME
        addTab(start)
    }

    private fun restoreTabs(state: Bundle) {
        val urls = state.getStringArrayList(KEY_URLS).orEmpty()
        if (urls.isEmpty()) {
            startFirstTab()
            return
        }
        urls.forEach { createTab(it, select = false) }
        currentIndex = state.getInt(KEY_ACTIVE, 0).coerceIn(0, tabs.lastIndex)
        geckoView.setSession(tabs[currentIndex].session)
        syncToolbar()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(KEY_URLS, ArrayList(tabs.map { it.url }))
        outState.putInt(KEY_ACTIVE, currentIndex)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentUrl(intent)?.let { addTab(it) }
    }

    override fun onResume() {
        super.onResume()
        if (ColdCatApp.extensionsChanged) {
            ColdCatApp.extensionsChanged = false
            currentTab?.session?.reload()
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            tabs.forEach { it.session.close() }
            CrashLog.endSession(this)
        }
        super.onDestroy()
    }

    private fun createTab(url: String, select: Boolean): Tab {
        val session = GeckoSession()
        val tab = Tab(session, url)
        tabs.add(tab)

        // The engine failed to start (see ColdCatApp.startupError). Keep the UI
        // alive so the diagnostics dialog stays reachable instead of crashing.
        if (ColdCatApp.startupError == null) {
            session.navigationDelegate = navigationDelegate
            session.progressDelegate = progressDelegate
            session.contentDelegate = contentDelegate
            CrashLog.checkpoint(this, "session:open")
            session.open(ColdCatApp.runtime)
            CrashLog.checkpoint(this, "session:load ${UrlUtils.pretty(url)}")
            session.loadUri(url)
        }

        if (select) {
            currentIndex = tabs.lastIndex
            geckoView.setSession(session)
            syncToolbar()
        }
        updateBadge()
        return tab
    }

    private fun addTab(url: String) {
        createTab(url, select = true)
        urlBar.clearFocus()
        hideKeyboard()
    }

    private fun switchTo(index: Int) {
        if (index !in tabs.indices || index == currentIndex) return
        currentIndex = index
        geckoView.setSession(tabs[index].session)
        syncToolbar()
    }

    private fun closeTab(index: Int) {
        if (index !in tabs.indices) return
        val removed = tabs.removeAt(index)
        removed.session.close()

        if (tabs.isEmpty()) {
            currentIndex = -1
            addTab(UrlUtils.HOME)
            return
        }

        if (index < currentIndex) {
            currentIndex--
        } else if (index == currentIndex) {
            currentIndex = index.coerceAtMost(tabs.lastIndex)
            geckoView.setSession(tabs[currentIndex].session)
            syncToolbar()
        }
        updateBadge()
    }

    private fun findTab(session: GeckoSession): Tab? = tabs.firstOrNull { it.session == session }

    private fun load(input: String) {
        val tab = currentTab ?: return
        val uri = UrlUtils.toUri(input, ColdCatApp.searchEngine(this))
        CrashLog.checkpoint(this, "user:load ${UrlUtils.pretty(uri)}")
        tab.session.loadUri(uri)
        urlBar.clearFocus()
        hideKeyboard()
    }

    private fun toggleLoading() {
        val tab = currentTab ?: return
        if (tab.loading) tab.session.stop() else tab.session.reload()
    }

    private fun syncToolbar() {
        val tab = currentTab ?: return
        urlBar.setText(if (UrlUtils.isHome(tab.url)) "" else tab.url)
        progressBar.visibility = if (tab.loading) View.VISIBLE else View.INVISIBLE
        reloadButton.setImageResource(if (tab.loading) R.drawable.ic_close else R.drawable.ic_reload)
        reloadButton.contentDescription = getString(if (tab.loading) R.string.stop else R.string.reload)
        updateNavigation()
        updateBadge()
    }

    private fun updateNavigation() {
        val tab = currentTab
        backButton.isEnabled = tab?.canGoBack == true
        backButton.alpha = if (backButton.isEnabled) 1f else 0.35f
        forwardButton.isEnabled = tab?.canGoForward == true
        forwardButton.alpha = if (forwardButton.isEnabled) 1f else 0.35f
    }

    private fun updateBadge() {
        tabBadge.text = tabs.size.toString()
        tabBadge.visibility = if (tabs.size > 1) View.VISIBLE else View.GONE
    }

    private fun showTabs() {
        if (tabs.isEmpty()) return
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.sheet_tabs, null)
        val list = view.findViewById<RecyclerView>(R.id.tabsList)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = TabsAdapter(
            items = tabs.toList(),
            selectedIndex = currentIndex,
            onSelect = { index ->
                dialog.dismiss()
                switchTo(index)
            },
            onClose = { index ->
                dialog.dismiss()
                closeTab(index)
            },
        )
        view.findViewById<TextView>(R.id.tabsTitle).text =
            getString(R.string.tabs) + " (" + tabs.size + ")"
        view.findViewById<ImageButton>(R.id.newTabButton).setOnClickListener {
            dialog.dismiss()
            addTab(UrlUtils.HOME)
        }
        dialog.setContentView(view)
        dialog.show()
    }

    private fun showMenu() {
        val tab = currentTab ?: return
        val desktop = tab.session.settings.userAgentMode == GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
        PopupMenu(this, findViewById(R.id.menuButton)).apply {
            menu.add(Menu.NONE, MENU_NEW_TAB, 0, R.string.new_tab)
            menu.add(Menu.NONE, MENU_ADDONS, 1, R.string.addons)
            menu.add(
                Menu.NONE,
                MENU_DESKTOP,
                2,
                if (desktop) R.string.mobile_site else R.string.desktop_site,
            )
            menu.add(Menu.NONE, MENU_SHARE, 3, R.string.share)
            menu.add(Menu.NONE, MENU_COPY, 4, R.string.copy_link)
            menu.add(Menu.NONE, MENU_SEARCH_ENGINE, 5, R.string.search_engine_menu)
            if (ColdCatApp.safeMode(this@MainActivity)) {
                menu.add(Menu.NONE, MENU_SAFE_MODE, 6, R.string.safe_mode_disable)
            }
            menu.add(Menu.NONE, MENU_DIAGNOSTICS, 7, R.string.menu_diagnostics)
            menu.add(Menu.NONE, MENU_ABOUT, 8, R.string.about)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    MENU_NEW_TAB -> addTab(UrlUtils.HOME)
                    MENU_ADDONS -> startActivity(Intent(this@MainActivity, AddonsActivity::class.java))
                    MENU_DESKTOP -> {
                        tab.session.settings.userAgentMode = if (desktop) {
                            GeckoSessionSettings.USER_AGENT_MODE_MOBILE
                        } else {
                            GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
                        }
                        tab.session.reload()
                    }
                    MENU_SHARE -> share(tab)
                    MENU_COPY -> copyLink(tab)
                    MENU_SEARCH_ENGINE -> showSearchEngineDialog()
                    MENU_SAFE_MODE -> disableSafeMode()
                    MENU_DIAGNOSTICS -> showDiagnostics(
                        getString(R.string.diagnostics_title),
                        diagnosticsText(),
                    )
                    MENU_ABOUT -> showAbout()
                }
                true
            }
            show()
        }
    }

    private fun share(tab: Tab) {
        if (UrlUtils.isHome(tab.url)) return
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, tab.url)
        startActivity(Intent.createChooser(send, getString(R.string.share_via)))
    }

    private fun copyLink(tab: Tab) {
        if (UrlUtils.isHome(tab.url)) return
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("url", tab.url))
        Toast.makeText(this, R.string.link_copied, Toast.LENGTH_SHORT).show()
    }

    private fun showAbout() {
        val view = layoutInflater.inflate(R.layout.dialog_about, null)
        view.findViewById<TextView>(R.id.aboutVersion).text =
            getString(R.string.about_version, BuildConfig.VERSION_NAME)
        MaterialAlertDialogBuilder(this)
            .setView(view)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun diagnosticsText(): String = buildString {
        append(CrashLog.deviceInfo(this@MainActivity))
        appendLine("Runtime: " + if (ColdCatApp.startupError == null) "ready" else "failed")
        appendLine("Safe mode: " + ColdCatApp.safeMode(this@MainActivity))
        appendLine("Search engine: " + ColdCatApp.searchEngine(this@MainActivity).title)
        appendLine("Previous run: " + if (CrashLog.previousUncleanExit == null) "clean shutdown" else "no normal shutdown")
        appendLine("Tabs: ${tabs.size}")
        appendLine("Start page: ${UrlUtils.HOME}")
        ColdCatApp.startupError?.let {
            appendLine()
            appendLine("Startup error:")
            append(it.stackTraceToString().lineSequence().take(12).joinToString("\n"))
        }
        CrashLog.read(this@MainActivity)?.lineSequence()?.take(25)?.joinToString("\n")?.let {
            appendLine()
            appendLine("Last crash (first lines):")
            append(it)
        }
        ExitInfo.recent(this@MainActivity).forEach { report ->
            appendLine()
            appendLine("System exit: ${report.reasonText} (${report.reason}) status=${report.status} at ${ExitInfo.format(report.timestamp)}")
            report.processName?.let { appendLine("Process: $it") }
            report.description?.takeIf { it.isNotBlank() }?.let { appendLine("Details: $it") }
            report.trace?.takeIf { it.isNotBlank() }?.let {
                appendLine("Trace:")
                append(it)
            }
        }
        appendLine()
        appendLine("Checkpoints:")
        append(CrashLog.checkpoints(this@MainActivity))
    }

    private fun showDiagnostics(title: String, body: String) {
        val shown = if (body.length > 6000) {
            body.take(6000) + "\n…\n(The full text is copied via the Copy button.)"
        } else {
            body
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(shown)
            .setPositiveButton(R.string.diagnostics_copy) { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText(title, body))
                Toast.makeText(this, R.string.diagnostics_copied, Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton(R.string.diagnostics_share) { _, _ -> shareReport(title, body) }
            .setNegativeButton(R.string.diagnostics_close, null)
            .show()
    }

    private fun shareReport(title: String, body: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        runCatching { startActivity(Intent.createChooser(intent, title)) }
    }

    private fun showSearchEngineDialog() {
        val engines = SearchEngine.entries
        val current = ColdCatApp.searchEngine(this)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.search_engine_title)
            .setSingleChoiceItems(
                engines.map { it.title }.toTypedArray(),
                engines.indexOf(current),
            ) { dialog, which ->
                ColdCatApp.setSearchEngine(this, engines[which])
                dialog.dismiss()
                Toast.makeText(
                    this,
                    getString(R.string.search_engine_set, engines[which].title),
                    Toast.LENGTH_SHORT,
                ).show()
            }
            .setNegativeButton(R.string.diagnostics_close, null)
            .show()
    }

    private fun disableSafeMode() {
        ColdCatApp.setSafeMode(this, false)
        Toast.makeText(this, R.string.safe_mode_disabled, Toast.LENGTH_LONG).show()
    }

    private fun hideKeyboard() {
        val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        manager.hideSoftInputFromWindow(urlBar.windowToken, 0)
    }

    private fun intentUrl(intent: Intent?): String? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_VIEW -> {
                val url = intent.dataString ?: return null
                if (url.startsWith("http://") || url.startsWith("https://")) url else null
            }
            // Opened from another app's "Share" sheet.
            Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("text/") != true) return null
                val shared = intent.getStringExtra(Intent.EXTRA_TEXT)
                    ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
                    ?: return null
                UrlUtils.firstHttpUrl(shared)
                    ?: shared.takeIf { it.isNotBlank() }
                        ?.let { UrlUtils.toUri(it, ColdCatApp.searchEngine(this)) }
            }
            else -> null
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private data class Tab(
        val session: GeckoSession,
        var url: String,
        var title: String = "",
        var canGoBack: Boolean = false,
        var canGoForward: Boolean = false,
        var loading: Boolean = false,
    )

    private inner class TabsAdapter(
        private val items: List<Tab>,
        private val selectedIndex: Int,
        private val onSelect: (Int) -> Unit,
        private val onClose: (Int) -> Unit,
    ) : RecyclerView.Adapter<TabsAdapter.Holder>() {

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.tabTitle)
            val url: TextView = view.findViewById(R.id.tabUrl)
            val close: ImageButton = view.findViewById(R.id.closeTabButton)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): Holder {
            return Holder(layoutInflater.inflate(R.layout.item_tab, parent, false))
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val tab = items[position]
            holder.title.text = tab.title.ifBlank {
                if (UrlUtils.isHome(tab.url)) getString(R.string.new_tab) else UrlUtils.pretty(tab.url)
            }
            holder.url.text = if (UrlUtils.isHome(tab.url)) {
                getString(R.string.home)
            } else {
                UrlUtils.pretty(tab.url)
            }
            holder.title.setTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    if (position == selectedIndex) R.color.ice_dark else R.color.text_primary,
                ),
            )
            holder.itemView.setOnClickListener { onSelect(position) }
            holder.close.setOnClickListener { onClose(position) }
        }

        override fun getItemCount(): Int = items.size
    }

    companion object {
        private const val KEY_URLS = "urls"
        private const val KEY_ACTIVE = "active"
        private const val MENU_NEW_TAB = 1
        private const val MENU_ADDONS = 2
        private const val MENU_DESKTOP = 3
        private const val MENU_SHARE = 4
        private const val MENU_COPY = 5
        private const val MENU_ABOUT = 6
        private const val MENU_DIAGNOSTICS = 7
        private const val MENU_SEARCH_ENGINE = 8
        private const val MENU_SAFE_MODE = 9

        private val INTERNAL_SCHEMES = setOf(
            "http", "https", "about", "resource", "file", "data", "blob", "moz-extension",
        )
    }
}
