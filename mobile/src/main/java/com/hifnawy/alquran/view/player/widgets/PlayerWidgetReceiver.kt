package com.hifnawy.alquran.view.player.widgets

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * A receiver for the [PlayerWidget] that is responsible for handling the lifecycle of the widget.
 *
 * This receiver is responsible for updating the widget when the user sends a notification from the widget.
 *
 * @see [PlayerWidget]
 */
class PlayerWidgetReceiver(override val glanceAppWidget: GlanceAppWidget = PlayerWidget()) : GlanceAppWidgetReceiver()