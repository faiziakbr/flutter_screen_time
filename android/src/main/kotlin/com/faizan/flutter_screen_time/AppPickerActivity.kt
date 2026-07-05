package com.faizan.flutter_screen_time

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast

class AppPickerActivity : Activity() {

    private data class AppEntry(val label: String, val packageName: String) {
        override fun toString(): String = label
    }

    private lateinit var listView: ListView
    private lateinit var entries: List<AppEntry>

    private companion object {
        const val MENU_SAVE = 1
        const val MENU_SELECT_ALL = 2
        const val MENU_UNSELECT_ALL = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        entries = loadLaunchableApps()

        listView = ListView(this).apply {
            choiceMode = ListView.CHOICE_MODE_MULTIPLE
            adapter = ArrayAdapter(
                this@AppPickerActivity,
                android.R.layout.simple_list_item_multiple_choice,
                entries,
            )
            // Let list items scroll into the inset padding instead of being
            // clipped, so the last rows aren't hidden by the navigation bar.
            clipToPadding = false
        }
        setContentView(listView)
        applySystemBarInsets(listView)

        // Pre-check the packages that are already blocked.
        val alreadyBlocked = ScreenTimePrefs.getBlockedPackages(this)
        entries.forEachIndexed { index, entry ->
            if (alreadyBlocked.contains(entry.packageName)) {
                listView.setItemChecked(index, true)
            }
        }
    }

    /**
     * Pad the list by the system bar insets so its content isn't hidden behind
     * the status bar or the (gesture/button) navigation bar on edge-to-edge devices.
     */
    @Suppress("DEPRECATION")
    private fun applySystemBarInsets(view: View) {
        view.setOnApplyWindowInsetsListener { v, insets ->
            v.setPadding(
                insets.systemWindowInsetLeft,
                insets.systemWindowInsetTop,
                insets.systemWindowInsetRight,
                insets.systemWindowInsetBottom,
            )
            insets
        }
        view.requestApplyInsets()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, MENU_SAVE, Menu.NONE, "Save").apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        menu.add(Menu.NONE, MENU_SELECT_ALL, Menu.NONE, "Select all")
        menu.add(Menu.NONE, MENU_UNSELECT_ALL, Menu.NONE, "Unselect all")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            MENU_SAVE -> {
                saveSelection()
                true
            }
            MENU_SELECT_ALL -> {
                setAllChecked(true)
                true
            }
            MENU_UNSELECT_ALL -> {
                setAllChecked(false)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setAllChecked(checked: Boolean) {
        for (i in entries.indices) {
            listView.setItemChecked(i, checked)
        }
    }

    private fun loadLaunchableApps(): List<AppEntry> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        return resolveInfos
            .map { info ->
                AppEntry(
                    label = info.loadLabel(pm).toString(),
                    packageName = info.activityInfo.packageName,
                )
            }
            // Don't offer the host app itself as a blockable target.
            .filter { it.packageName != packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    private fun saveSelection() {
        val checked = listView.checkedItemPositions
        val selected = mutableSetOf<String>()
        for (i in 0 until checked.size()) {
            if (checked.valueAt(i)) {
                selected.add(entries[checked.keyAt(i)].packageName)
            }
        }
        ScreenTimePrefs.setBlockedPackages(this, selected)
        Toast.makeText(
            this,
            "Saved ${selected.size} app(s) to block",
            Toast.LENGTH_SHORT,
        ).show()
        setResult(Activity.RESULT_OK)
        finish()
    }
}
