@file:Suppress("DEPRECATION")

package org.securesystem.insular.settings

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.app.Activity
import android.app.admin.DevicePolicyManager.ACTION_PROVISION_MANAGED_DEVICE
import android.app.admin.DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.*
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.*
import android.os.Bundle
import android.preference.Preference
import android.preference.TwoStatePreference
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import org.securesystem.insular.appops.AppOpsCompat
import org.securesystem.insular.mobile.R
import org.securesystem.insular.notification.NotificationIds
import org.securesystem.insular.setup.IslandSetup
import org.securesystem.insular.shuttle.PendingIntentShuttle
import org.securesystem.insular.util.DevicePolicies
import org.securesystem.insular.util.Modules
import org.securesystem.insular.util.Users

/**
 * Settings for each managed profile, also as launcher activity in managed profile.
 *
 * Created by Oasis on 2019-10-12.
 */
class IslandSettingsFragment: android.preference.PreferenceFragment() {

    override fun onResume() {
        super.onResume()
        val policies = DevicePolicies(activity)
        val isProfileOrDeviceOwner = policies.isProfileOrDeviceOwnerOnCallingUser
        if (Users.isOwner() && ! isProfileOrDeviceOwner) {
            setup<Preference>(R.string.key_device_owner_setup) {
                summary = getString(R.string.pref_device_owner_summary) + getString(R.string.pref_device_owner_featurs)
                setOnPreferenceClickListener { true.also {
                    IslandSetup.requestDeviceOwnerActivation(this@IslandSettingsFragment, REQUEST_DEVICE_OWNER_ACTIVATION) }}}
            setup<Preference>(R.string.key_privacy) { isEnabled = false }   // Show but disabled, as a feature preview.
            setup<Preference>(R.string.key_watcher) { isEnabled = false }
            setup<Preference>(R.string.key_island_watcher) { remove(this) }
            setup<Preference>(R.string.key_setup) { remove(this) }
            return
        }
        setup<Preference>(R.string.key_device_owner_setup) { remove(this) }
        setupPreferenceForManagingAppOps(R.string.key_manage_read_phone_state, READ_PHONE_STATE, AppOpsCompat.OP_READ_PHONE_STATE,
                R.string.pref_privacy_read_phone_state_title, SDK_INT <= P)
        setupPreferenceForManagingAppOps(R.string.key_manage_read_sms, READ_SMS, AppOpsCompat.OP_READ_SMS,
                R.string.pref_privacy_read_sms_title)
        setupPreferenceForManagingAppOps(R.string.key_manage_location, ACCESS_COARSE_LOCATION, AppOpsCompat.OP_COARSE_LOCATION,
                R.string.pref_privacy_location_title)
        if (Settings.Global.getInt(activity.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 0)
            setup<Preference>(R.string.key_device_owner_setup) { remove(this) }
        else setupPreferenceForManagingAppOps(R.string.key_manage_storage, READ_EXTERNAL_STORAGE,
                AppOpsCompat.OP_READ_EXTERNAL_STORAGE, R.string.pref_privacy_storage_title)
        setupNotificationChannelTwoStatePreference(R.string.key_island_watcher, SDK_INT >= P && ! Users.isOwner(), NotificationIds.IslandWatcher)
        setupNotificationChannelTwoStatePreference(R.string.key_app_watcher, SDK_INT >= O, NotificationIds.IslandAppWatcher)
        setup<Preference>(R.string.key_reprovision) {
            if (Users.isOwner() && ! isProfileOrDeviceOwner) return@setup remove(this)
            setOnPreferenceClickListener { true.also { @SuppressLint("InlinedApi")
                val action = if (policies.isActiveDeviceOwner) ACTION_PROVISION_MANAGED_DEVICE else ACTION_PROVISION_MANAGED_PROFILE
                ContextCompat.startForegroundService(activity, Intent(action).setPackage(Modules.MODULE_ENGINE)) }}}
        setup<Preference>(R.string.key_destroy) {
            if (Users.isOwner()) {
                if (! isProfileOrDeviceOwner) return@setup remove(this)
                setTitle(R.string.pref_rescind_title)
                summary = getString(R.string.pref_rescind_summary) + getString(R.string.pref_device_owner_featurs) + "\n" }
            setOnPreferenceClickListener { true.also {
                if (Users.isOwner()) IslandSetup.requestDeviceOrProfileOwnerDeactivation(activity)
                else IslandSetup.requestProfileRemoval(activity) }}}
    }

    private fun setupPreferenceForManagingAppOps(key: Int, permission: String, op: Int, @StringRes prompt: Int, precondition: Boolean = true) {
        setup<Preference>(key) {
            if (SDK_INT >= P && precondition) {
                setOnPreferenceClickListener { true.also { OpsManager(activity, permission, op).startOpsManager(prompt) }}
            } else remove(this) }
    }

    private fun setupNotificationChannelTwoStatePreference(@StringRes key: Int, visible: Boolean, notificationId: NotificationIds) {
        setup<TwoStatePreference>(key) {
            if (visible && SDK_INT >= O) {
                isChecked = ! notificationId.isBlocked(context)
                setOnPreferenceChangeListener { _,_ -> true.also { context.startActivity(notificationId.buildChannelSettingsIntent(context)) }}
            } else remove(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        activity.actionBar?.setDisplayHomeAsUpEnabled(true)
        addPreferencesFromResource(R.xml.pref_island)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId != android.R.id.home) super.onOptionsItemSelected(item) else true.also { activity.finish() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_DEVICE_OWNER_ACTIVATION) IslandSetup.onAddAdminResult(activity)
    }
}

class IslandSettingsActivity: Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
	    if (SDK_INT >= M) {
            val shuttle = PendingIntentShuttle.retrieveFromActivity(this)
            if (shuttle != null) { return finish().also { Log.i(TAG, "Shuttle received: $shuttle") }}}

        title = intent?.getStringExtra(Intent.EXTRA_TITLE)
                ?: getString(R.string.tab_island).let { if (Users.current() == Users.profile) it else "$it (${Users.toId(Users.current())})"}
        fragmentManager.beginTransaction().replace(android.R.id.content, IslandSettingsFragment()).commit()
    }

    class Enabler: BroadcastReceiver() {    // One-time enabler for

        override fun onReceive(context: Context, intent: Intent) {      // ACTION_LOCKED_BOOT_COMPLETED is unnecessary for activity
            if (Intent.ACTION_BOOT_COMPLETED == intent.action || Intent.ACTION_MY_PACKAGE_REPLACED == intent.action) context.packageManager.apply {
                if (Users.isOwner()) return         // Not needed in mainland
                setComponentEnabledSetting(ComponentName(context, IslandSettingsActivity::class.java), COMPONENT_ENABLED_STATE_ENABLED, DONT_KILL_APP)
                setComponentEnabledSetting(ComponentName(context, Enabler::class.java), COMPONENT_ENABLED_STATE_DISABLED, DONT_KILL_APP)
            }
        }
    }
}

private const val REQUEST_DEVICE_OWNER_ACTIVATION = 1

private const val TAG = "Island.SA"
