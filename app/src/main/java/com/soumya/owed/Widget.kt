package com.soumya.owed

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.RemoteViews

class BalanceWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        update(context)
    }

    companion object {
        fun update(ctx: Context) {
            val store = Store(ctx)
            LocaleHub.lang = store.language()
            LocaleHub.currency = store.currency()
            val open = store.load().filter { !it.returned }
            val toYou = open.filter { it.isMoney && it.direction == LENT }.sumOf { owing(it) }
            val youOwe = open.filter { it.isMoney && it.direction == BORROWED }.sumOf { owing(it) }
            val night = when (store.appearance()) {
                "dark" -> true
                "light" -> false
                else -> (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
            }
            val label = if (night) 0xFF8E8E93.toInt() else 0xFF8E8E93.toInt()
            val title = if (night) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
            val views = RemoteViews(ctx.packageName, R.layout.widget_balance)
            views.setInt(
                R.id.widget_root,
                "setBackgroundResource",
                if (night) R.drawable.widget_bg_dark else R.drawable.widget_bg
            )
            views.setTextColor(R.id.widget_brand, label)
            views.setTextColor(R.id.widget_to_you_label, label)
            views.setTextColor(R.id.widget_you_owe_label, label)
            views.setTextColor(R.id.widget_open, label)
            views.setTextViewText(R.id.widget_brand, "Owed")
            views.setTextViewText(R.id.widget_to_you_label, t("Owed to you"))
            views.setTextViewText(R.id.widget_you_owe_label, t("You owe"))
            views.setTextViewText(R.id.widget_to_you, inr(toYou))
            views.setTextViewText(R.id.widget_you_owe, inr(youOwe))
            views.setTextColor(R.id.widget_to_you, 0xFF007AFF.toInt())
            views.setTextColor(R.id.widget_you_owe, if (youOwe > 0.5) 0xFFFF3B30.toInt() else title)
            val count = open.size
            views.setTextViewText(
                R.id.widget_open,
                if (count == 1) t("1 open") else t("%d open", count)
            )
            val launch = PendingIntent.getActivity(
                ctx,
                70,
                Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, launch)
            val manager = AppWidgetManager.getInstance(ctx)
            val ids = manager.getAppWidgetIds(ComponentName(ctx, BalanceWidget::class.java))
            if (ids.isNotEmpty()) manager.updateAppWidget(ids, views)
        }
    }
}
