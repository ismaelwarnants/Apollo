package org.nuclearfog.apollo.receiver;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.nuclearfog.apollo.service.MusicPlaybackService;
import org.nuclearfog.apollo.ui.widgets.AppWidgetBase;
import org.nuclearfog.apollo.ui.widgets.AppWidgetLarge;
import org.nuclearfog.apollo.ui.widgets.AppWidgetLargeAlternate;
import org.nuclearfog.apollo.ui.widgets.AppWidgetSmall;
import org.nuclearfog.apollo.ui.widgets.RecentWidgetProvider;

/**
 * widget Broadcast listener used to update all widgets
 *
 * @author nuclearfog
 */
public class WidgetBroadcastReceiver extends BroadcastReceiver {

	/**
	 *
	 */
	public static final String WIDGET_TYPE = "widget_type";

	private AppWidgetBase smallWidget = new AppWidgetSmall();
	private AppWidgetBase largeWidget = new AppWidgetLarge();
	private AppWidgetBase altWidget = new AppWidgetLargeAlternate();
	private AppWidgetBase recentWidget = new RecentWidgetProvider();

	private MusicPlaybackService service;

	/**
	 * @param service callback to playback service
	 */
	public WidgetBroadcastReceiver(MusicPlaybackService service) {
		this.service = service;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		// type of widget to update
		String type = intent.getStringExtra(WIDGET_TYPE);
		// IDs of the widgets to update
		int[] small = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
		if (AppWidgetSmall.CMDAPPWIDGETUPDATE.equals(type)) {
			smallWidget.performUpdate(service, small);
		} else if (AppWidgetLarge.CMDAPPWIDGETUPDATE.equals(type)) {
			largeWidget.performUpdate(service, small);
		} else if (AppWidgetLargeAlternate.CMDAPPWIDGETUPDATE.equals(type)) {
			altWidget.performUpdate(service, small);
		} else if (RecentWidgetProvider.CMDAPPWIDGETUPDATE.equals(type)) {
			recentWidget.performUpdate(service, small);
		}
	}

	/**
	 * update app widgets
	 */
	public void updateWidgets(MusicPlaybackService service, String what) {
		smallWidget.notifyChange(service, what);
		largeWidget.notifyChange(service, what);
		altWidget.notifyChange(service, what);
		recentWidget.notifyChange(service, what);
	}
}