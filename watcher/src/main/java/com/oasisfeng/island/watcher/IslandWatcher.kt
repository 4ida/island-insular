package com.oasisfeng.island.watcher

import android.app.*
import android.app.Notification.CATEGORY_STATUS
import android.app.Notification.VISIBILITY_PUBLIC
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.admin.DevicePolicyManager
import android.app.admin.DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER
import android.content.*
import android.content.Intent.CATEGORY_DEFAULT
import android.content.Intent.CATEGORY_HOME
import android.content.pm.LauncherApps
import android.content.pm.PackageManager.*
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.os.*
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import android.os.Build.VERSION_CODES.P
import android.os.Build.VERSION_CODES.Q
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import com.oasisfeng.android.widget.Toasts
import com.oasisfeng.hack.Hack
import com.oasisfeng.island.notification.NotificationIds
import com.oasisfeng.island.notification.post
import com.oasisfeng.island.shuttle.Shuttle
import com.oasisfeng.island.util.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Watch recently started managed-profile and offer action to stop.
 *
 * Created by Oasis on 2019-2-25.
 */
@RequiresApi(P) class IslandWatcher : BroadcastReceiver() {

	override fun onReceive(context: Context, intent: Intent) {
		Log.d(TAG, "onReceive: $intent")
		if (SDK_INT < P || intent.action !in listOf(Intent.ACTION_LOCKED_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
						NotificationManager.ACTION_NOTIFICATION_CHANNEL_BLOCK_STATE_CHANGED,
						NotificationManager.ACTION_APP_BLOCK_STATE_CHANGED)) return
		if (Users.isParentProfile()) return context.packageManager.setComponentEnabledSetting(ComponentName(context, javaClass),
				COMPONENT_ENABLED_STATE_DISABLED, DONT_KILL_APP)
		if (SDK_INT < O) return
		val policies = DevicePolicies(context)
		if (! policies.isProfileOwner) return

		val hasShortcutHostPermission = context.getSystemService(LauncherApps::class.java)!!.hasShortcutHostPermission()
		if (NotificationIds.IslandWatcher.isBlocked(context)) return

		val supportsDeactivate = if (SDK_INT < Q) hasShortcutHostPermission else isParentProfileOwner(context)
		val supportsRestart = ! policies.invoke(DPM::isUsingUnifiedPassword)
				&& policies.manager.storageEncryptionStatus == ENCRYPTION_STATUS_ACTIVE_PER_USER

		NotificationIds.IslandWatcher.post(context) {
			setOngoing(true).setGroup(GROUP).setGroupSummary(true)
			setSmallIcon(R.drawable.ic_landscape_black_24dp).setLargeIcon(Icon.createWithBitmap(getAppIcon(context)))
			setColor(context.getColor(R.color.primary)).setCategory(CATEGORY_STATUS).setVisibility(VISIBILITY_PUBLIC)
			setContentTitle(context.getText(R.string.notification_island_watcher_title))
			setContentText(context.getText(if (supportsDeactivate) R.string.notification_island_watcher_text_for_deactivate
					else R.string.notification_island_watcher_text_for_restart))
			if (supportsDeactivate) addServiceAction(context, R.string.action_deactivate_island)
			if (supportsRestart) addServiceAction(context, R.string.action_restart_island, Intent.ACTION_REBOOT)
			if (! supportsDeactivate && ! supportsRestart)
				addServiceAction(context, R.string.action_deactivate_island, Settings.ACTION_SYNC_SETTINGS)
			addAction(Notification.Action.Builder(null, context.getText(R.string.action_settings), PendingIntent.getActivity(context, 0,
				NotificationIds.IslandWatcher.buildChannelSettingsIntent(context), FLAG_UPDATE_CURRENT)).build()) }
	}

	private fun Notification.Builder.addServiceAction(context: Context, @StringRes label: Int, action: String? = null) {
		addAction(Notification.Action.Builder(null, context.getText(label), PendingIntent.getService(context, 0,
			Intent(context, IslandDeactivationService::class.java).setAction(action), FLAG_UPDATE_CURRENT)).build())
	}

	private fun isParentProfileOwner(context: Context) =
		try { Shuttle(context, to = Users.getParentProfile()).invoke { DevicePolicies(this).isProfileOwner }}
		catch (e: IllegalStateException) { false }

	private fun getAppIcon(context: Context): Bitmap {
		val size = context.resources.getDimensionPixelSize(android.R.dimen.app_icon_size)
		return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
			context.applicationInfo.loadIcon(context.packageManager).apply {
				setBounds(0, 0, size, size)
				draw(Canvas(bitmap)) }}
	}

	@RequiresApi(P) class IslandDeactivationService : Service() {

		override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
			val action = intent?.action
			if (action == Intent.ACTION_REBOOT) {
				DevicePolicies(this).manager.lockNow(DevicePolicyManager.FLAG_EVICT_CREDENTIAL_ENCRYPTION_KEY) }
			else if (action == Settings.ACTION_SYNC_SETTINGS)
				try {
					startActivity(Intent(Settings.ACTION_SYNC_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
					Toasts.showLong(applicationContext, R.string.toast_manual_quiet_mode) }
				catch (_: ActivityNotFoundException) { Toasts.showLong(applicationContext, "Sorry, ROM is incompatible.") }
			else if (SDK_INT >= Q) {
				if (Users.isParentProfile()) {
					intent?.getParcelableExtra<UserHandle>(Intent.EXTRA_USER)?.also { profile ->
						GlobalScope.launch { requestQuietModeApi29(this@IslandDeactivationService, profile) }
						return START_STICKY }}   // Still ongoing
				else Shuttle(this, to = Users.getParentProfile()).launch(with = Users.current()) {
					startService(Intent(this, IslandDeactivationService::class.java).putExtra(Intent.EXTRA_USER, it)) }}
			else requestQuietMode(Users.current())
			stopSelf()
			return START_NOT_STICKY
		}

		@OwnerUser @RequiresApi(Q) private suspend fun requestQuietModeApi29(context: Context, profile: UserHandle) {
			if (! Users.isProfileManagedByIsland()) return startSystemSyncSettings()

			Log.i(TAG, "Preparing to deactivating Island (${profile.toId()})...")
			val dummyHome = ComponentName(context, DummyHomeActivity::class.java); val pm = context.packageManager
			pm.setComponentEnabledSetting(dummyHome, COMPONENT_ENABLED_STATE_ENABLED, DONT_KILL_APP)
			for (index in 0 until 10) {
				Log.i(TAG, "Acquiring default home...")
				if (! makeDefaultHome(dummyHome)) {     // It may not work for the first few times,
					delay(500); continue }              //   just try again in a short delay.

				Log.i(TAG, "Deactivating Island ${profile.toId()}...")
				val result = waitBroadcast(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE) {
					requestQuietMode(profile) }

				pm.setComponentEnabledSetting(dummyHome, COMPONENT_ENABLED_STATE_DISABLED, DONT_KILL_APP)
				val user = result.getParcelableExtra<UserHandle>(Intent.EXTRA_USER)
				Log.i(TAG, "Island is deactivated: ${user?.toId()}")
				return Toasts.showShort(context, "Island is deactivated.") }
		}

		private fun makeDefaultHome(home: ComponentName): Boolean {
			DevicePolicies(this).execute(DevicePolicyManager::clearPackagePersistentPreferredActivities, packageName)
			DevicePolicies(this).execute(DevicePolicyManager::addPersistentPreferredActivity,
					IntentFilter(Intent.ACTION_MAIN).apply { addCategory(CATEGORY_HOME); addCategory(CATEGORY_DEFAULT) }, home)
			return getDefaultHome() == home
		}

		private fun getDefaultHome()
				= Hack.into(packageManager).with(Hacks.PackageManagerHack::class.java).getHomeActivities(ArrayList<ResolveInfo>())

		private suspend fun waitBroadcast(action: String, block: () -> Unit): Intent = suspendCoroutine { continuation ->
			registerReceiver(object : BroadcastReceiver() { override fun onReceive(_context: Context, intent: Intent) {
				unregisterReceiver(this)
				continuation.resume(intent)
			}}, IntentFilter(action))
			block()
		}

		private fun requestQuietMode(profile: UserHandle) {
			// requestQuietModeEnabled() requires us running as foreground (service).
			NotificationIds.IslandWatcher.startForeground(this, Notification.Builder(this, null)
					.setSmallIcon(R.drawable.ic_landscape_black_24dp).setColor(getColor(R.color.primary)).setCategory(Notification.CATEGORY_PROGRESS)
					.setProgress(0, 0, true).setContentTitle("Deactivating Island space..."))

			try { getSystemService(UserManager::class.java)!!.requestQuietModeEnabled(true, profile) }
			catch (e: SecurityException) {   // Fall-back to manual control
				startSystemSyncSettings().also { Log.d(TAG, "Error deactivating Island ${profile.toId()}", e) }}
			finally { stopForeground(true) }
		}

		private fun startSystemSyncSettings() {
			try {
				startActivity(Intent(Settings.ACTION_SYNC_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
				Toasts.showLong(applicationContext, R.string.toast_manual_quiet_mode) }
			catch (_: ActivityNotFoundException) { Toasts.showLong(applicationContext, "Sorry, ROM is incompatible.") }
		}

		override fun onBind(intent: Intent): IBinder? = null
	}

	class DummyHomeActivity : Activity() {
		override fun onCreate(savedInstanceState: Bundle?) {
			super.onCreate(savedInstanceState)
			if (Users.isParentProfile()) packageManager.setComponentEnabledSetting( // In case of unexpected interruption
					ComponentName(this, javaClass), COMPONENT_ENABLED_STATE_DISABLED, DONT_KILL_APP)
			startActivity(Intent(Intent.ACTION_MAIN).addCategory(CATEGORY_HOME))
			finish()
		}
	}
}

// With shared notification group, app watcher (group child) actually hides Island watcher (group summary), which only shows up if no app watchers.
internal const val GROUP = "Watcher"
private const val TAG = "Island.Watcher"
