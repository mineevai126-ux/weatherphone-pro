package com.example.weatherphonepro

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

class SerzWeatherWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.serz_weather_widget)
            views.setTextViewText(R.id.serz_widget_title, "Суперпрогноз Serz")
            views.setTextViewText(R.id.serz_widget_temp, "Откройте приложение")
            views.setTextViewText(R.id.serz_widget_note, "точность · риск осадков · Serz")
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
