package com.t9search.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.widget.RemoteViews

class T9WidgetProvider : AppWidgetProvider() {

    companion object {
        private const val ACTION_KEY = "com.t9search.KEY"
        private const val EXTRA_DIGIT = "digit"
        private const val ACTION_LAUNCH = "com.t9search.LAUNCH"
        private const val EXTRA_PACKAGE = "pkg"
        private const val PREFS = "t9_prefs"
        private const val KEY_DIGITS = "last_digits"

        private val T9_MAP = mapOf(
            '2' to "abc", '3' to "def", '4' to "ghi",
            '5' to "jkl", '6' to "mno", '7' to "pqrs",
            '8' to "tuv", '9' to "wxyz"
        )
    }

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        val digits = context.getSharedPreferences(PREFS, 0).getString(KEY_DIGITS, "") ?: ""
        ids.forEach { updateWidget(context, mgr, it, digits) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_KEY -> {
                val digit = intent.getStringExtra(EXTRA_DIGIT) ?: return
                val prefs = context.getSharedPreferences(PREFS, 0)
                val current = prefs.getString(KEY_DIGITS, "") ?: ""
                val newDigits = if (digit == "C") "" else current + digit
                prefs.edit().putString(KEY_DIGITS, newDigits).apply()
                refreshAll(context)
            }
            ACTION_LAUNCH -> {
                val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: return
                val launch = context.packageManager.getLaunchIntentForPackage(pkg)
                launch?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it)
                }
            }
        }
    }

    private fun refreshAll(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, T9WidgetProvider::class.java))
        val digits = context.getSharedPreferences(PREFS, 0).getString(KEY_DIGITS, "") ?: ""
        ids.forEach { updateWidget(context, mgr, it, digits) }
    }

    private fun updateWidget(context: Context, mgr: AppWidgetManager, id: Int, digits: String) {
        val views = RemoteViews(context.packageName, R.layout.widget_t9)
        setupKeypad(context, views)

        val results = if (digits.isNotEmpty()) searchApps(context, digits) else emptyList()
        val iconIds = intArrayOf(R.id.icon_0, R.id.icon_1, R.id.icon_2)
        val labelIds = intArrayOf(R.id.label_0, R.id.label_1, R.id.label_2)
        val slotIds = intArrayOf(R.id.result_0, R.id.result_1, R.id.result_2)

        for (i in 0..2) {
            if (i < results.size) {
                val (label, pkg, icon) = results[i]
                views.setImageViewBitmap(iconIds[i], drawableToBitmap(icon))
                views.setTextViewText(labelIds[i], label)
                views.setOnClickPendingIntent(slotIds[i], launchIntent(context, pkg, i))
            } else {
                views.setImageViewBitmap(iconIds[i], null)
                views.setTextViewText(labelIds[i], "")
                views.setOnClickPendingIntent(slotIds[i], null)
            }
        }
        mgr.updateAppWidget(id, views)
    }

    private fun setupKeypad(context: Context, views: RemoteViews) {
        val buttons = mapOf(
            R.id.btn_clear to "C", R.id.btn_2 to "2", R.id.btn_3 to "3",
            R.id.btn_4 to "4", R.id.btn_5 to "5", R.id.btn_6 to "6",
            R.id.btn_7 to "7", R.id.btn_8 to "8", R.id.btn_9 to "9"
        )
        buttons.forEach { (btnId, digit) ->
            val intent = Intent(context, T9WidgetProvider::class.java).apply {
                action = ACTION_KEY
                putExtra(EXTRA_DIGIT, digit)
            }
            val pi = PendingIntent.getBroadcast(
                context, btnId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setOnClickPendingIntent(btnId, pi)
        }
    }

    private fun launchIntent(context: Context, pkg: String, idx: Int): PendingIntent {
        val intent = Intent(context, T9WidgetProvider::class.java).apply {
            action = ACTION_LAUNCH
            putExtra(EXTRA_PACKAGE, pkg)
        }
        return PendingIntent.getBroadcast(
            context, 100 + idx, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    private fun searchApps(context: Context, digits: String): List<Triple<String, String, Drawable>> {
        val pm = context.packageManager
        val apps = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        )
        return apps.mapNotNull { ri ->
            val label = ri.loadLabel(pm).toString()
            val score = t9FuzzyScore(label.lowercase(), digits)
            if (score > 0) Triple(label, ri.activityInfo.packageName, ri.loadIcon(pm)) to score
            else null
        }.sortedByDescending { it.second }
            .take(3)
            .map { it.first }
    }

    private fun t9FuzzyScore(name: String, digits: String): Int {
        // Match digits sequentially against characters in the name (fuzzy/subsequence)
        var di = 0
        var score = 0
        var consecutive = 0
        for (ch in name) {
            if (di >= digits.length) break
            val expected = T9_MAP[digits[di]]
            if (expected != null && ch in expected) {
                di++
                consecutive++
                score += consecutive // reward consecutive matches
            } else {
                consecutive = 0
            }
        }
        return if (di == digits.length) score else 0
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val bmp = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, 96, 96)
        drawable.draw(canvas)
        return bmp
    }
}
