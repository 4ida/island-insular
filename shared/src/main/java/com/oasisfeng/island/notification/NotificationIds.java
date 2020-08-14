package com.oasisfeng.island.notification;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationManagerCompat;

import com.oasisfeng.island.shared.R;

import java.util.function.Consumer;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.app.NotificationManager.IMPORTANCE_MIN;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.O;
import static android.provider.Settings.EXTRA_APP_PACKAGE;
import static java.util.Objects.requireNonNull;

/**
 * Central definition for all notification IDs, to avoid conflicts.
 *
 * Created by Oasis on 2016/11/28.
 */
public enum NotificationIds {

	// New ID must be added to the end, to preserve the ordinal
	Provisioning(Channel.OngoingTask),
	UninstallHelper(Channel.Important),
	AppInstallation(Channel.AppInstall),
	IslandWatcher(Channel.Watcher),
	IslandAppWatcher(Channel.AppWatcher),
	Authorization(Channel.Important),
	Debug(Channel.Debug, 999);

	public void post(final Context context, final Notification.Builder notification) {
		NotificationManagerCompat.from(context).notify(id(), buildChannel(context, notification).build());
	}

	public void post(final Context context, final String tag, final Notification.Builder notification) {
		NotificationManagerCompat.from(context).notify(tag, id(), buildChannel(context, notification).build());
	}

	public void cancel(final Context context) {
		NotificationManagerCompat.from(context).cancel(id());
	}

	public void cancel(final Context context, final String tag) {
		NotificationManagerCompat.from(context).cancel(tag, id());
	}

	public boolean isBlocked(final Context context) {
		final NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		if (nm == null || ! nm.areNotificationsEnabled()) return true;
		if (SDK_INT < O) return false;
		final NotificationChannel actual_channel = nm.getNotificationChannel(channel.name);
		return actual_channel != null && actual_channel.getImportance() == NotificationManager.IMPORTANCE_NONE;
	}

	public static boolean isBlockedByApp(final Context context) {
		final NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		return nm == null || ! nm.areNotificationsEnabled();
	}

	public void startForeground(final Service service, final Notification.Builder notification) {
		service.startForeground(id(), buildChannel(service, notification).build());
	}

	Notification.Builder buildChannel(final Context context, final Notification.Builder notification) {
		if (SDK_INT < O) return notification;
		return notification.setChannelId(channel.createAndGetId(context));
	}

	int id() { return id != 0 ? id : ordinal() + 1; }		// 0 is reserved

	public Intent buildChannelSettingsIntent(final Context context) {
		if (SDK_INT >= O) channel.createAndGetId(context);
		if (SDK_INT < O || isBlockedByApp(context))
			return new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(EXTRA_APP_PACKAGE, context.getPackageName());
		return new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(EXTRA_APP_PACKAGE, context.getPackageName())
				.putExtra(Settings.EXTRA_CHANNEL_ID, channel.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	}

	NotificationIds(final Channel channel) { this(channel, 0); }
	NotificationIds(final Channel channel, final int id) { this.channel = channel; this.id = id; }

	private final Channel channel;
	private final int id;

	@SuppressLint("InlinedApi") enum Channel {

		OngoingTask	("OngoingTask",	R.string.notification_channel_ongoing_task,	IMPORTANCE_HIGH, 	channel -> channel.setShowBadge(false)),
		Important	("Important",	R.string.notification_channel_important,	IMPORTANCE_HIGH, 	channel -> channel.setShowBadge(true)),
		AppInstall	("AppInstall",	R.string.notification_channel_app_install,	IMPORTANCE_HIGH, 	c -> { c.setShowBadge(true); c.setSound(null, null);}),
		Watcher		("Watcher",		R.string.notification_channel_island_watcher,		IMPORTANCE_DEFAULT, c -> c.setShowBadge(true), c -> c.setSound(null, null)),
		AppWatcher	("AppWatcher",	R.string.notification_channel_app_watcher,	IMPORTANCE_DEFAULT, c -> c.setShowBadge(true), c -> c.setSound(null, null)),
		Debug		("Debug",		R.string.notification_channel_debug,		IMPORTANCE_MIN,  	channel -> channel.setShowBadge(false));

		@SafeVarargs Channel(final String name, final @StringRes int title, final int importance, final @Nullable Consumer<NotificationChannel>... tweaks) {
			this.name = name; this.title = title; this.importance = importance; this.tweaks = tweaks;
		}

		@RequiresApi(O) String createAndGetId(final Context context) {
			if (! created) {
				final NotificationChannel channel = new NotificationChannel(name, context.getString(title), importance);
				if (tweaks != null) for (final Consumer<NotificationChannel> tweak : tweaks)
					tweak.accept(channel);
				requireNonNull(context.getSystemService(NotificationManager.class)).createNotificationChannel(channel);
				created = true;
			}
			return name;
		}

		private final String name;
		private final @StringRes int title;
		private final int importance;
		private final @Nullable Consumer<NotificationChannel>[] tweaks;
		private boolean created;
	}
}
