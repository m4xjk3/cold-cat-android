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
            val scheme = Uri.parse(request.uri).scheme?.lowercase() ?: return null
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

        if (savedInstanceState != null) restoreTabs(savedInstanceState) else startFirstTab()
        updateNavigation()
        updateBadge()
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
        if (isFinishing) tabs.forEach { it.session.close() }
        super.onDestroy()
    }

    private fun createTab(url: String, select: Boolean): Tab {
        val session = GeckoSession()
        val tab = Tab(session, url)
        tabs.add(tab)
        session.navigationDelegate = navigationDelegate
        session.progressDelegate = progressDelegate
        session.contentDelegate = contentDelegate
        session.open(ColdCatApp.runtime)
        session.loadUri(url)

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
        tab.session.loadUri(UrlUtils.toUri(input))
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
            menu.add(Menu.NONE, MENU_ABOUT, 5, R.string.about)
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

    private fun hideKeyboard() {
        val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        manager.hideSoftInputFromWindow(urlBar.windowToken, 0)
    }

    private fun intentUrl(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_VIEW) return null
        val url = intent.dataString ?: return null
        return if (url.startsWith("http://") || url.startsWith("https://")) url else null
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

        private val INTERNAL_SCHEMES = setOf(
            "http", "https", "about", "resource", "file", "data", "blob", "moz-extension",
        )
    }
}
