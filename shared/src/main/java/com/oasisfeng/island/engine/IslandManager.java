package com.oasisfeng.island.engine;

import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;

import com.oasisfeng.android.annotation.UserIdInt;
import com.oasisfeng.android.os.UserHandles;
import com.oasisfeng.android.util.Apps;
import com.oasisfeng.island.util.DevicePolicies;
import com.oasisfeng.island.util.Hacks;
import com.oasisfeng.island.util.OwnerUser;
import com.oasisfeng.island.util.ProfileUser;
import com.oasisfeng.island.util.Users;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.O;
import static java.util.Objects.requireNonNull;

/**
 * Utilities of shared basic functionality for modules
 *
 * Created by Oasis on 2017/2/20.
 */
@ParametersAreNonnullByDefault
public class IslandManager {

	public static boolean ensureLegacyInstallNonMarketAppAllowed(final Context context, final DevicePolicies policies) {
		policies.clearUserRestrictionsIfNeeded(context, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES);
		if (SDK_INT >= O) return true;		// INSTALL_NON_MARKET_APPS is no longer supported since Android O.

		final ContentResolver resolver = context.getContentResolver();
		@SuppressWarnings("deprecation") final String INSTALL_NON_MARKET_APPS = Settings.Secure.INSTALL_NON_MARKET_APPS;
		if (Settings.Secure.getInt(resolver, INSTALL_NON_MARKET_APPS, 0) > 0) return true;
		policies.execute(DevicePolicyManager::setSecureSetting, INSTALL_NON_MARKET_APPS, "1");
		return Settings.Secure.getInt(resolver, INSTALL_NON_MARKET_APPS, 0) > 0;
	}

	@OwnerUser @ProfileUser public static boolean ensureAppHiddenState(final Context context, final String pkg, final boolean state) {
		final DevicePolicies policies = new DevicePolicies(context);
		if (policies.setApplicationHidden(pkg, state)) return true;
		// Since setApplicationHidden() return false if already in that state, also check the current state.
		final boolean hidden = policies.invoke(DevicePolicyManager::isApplicationHidden, pkg);
		return state == hidden;
	}

	/** @return error information, or empty string for success. */
	@OwnerUser @ProfileUser public static @NonNull String ensureAppFreeToLaunch(final Context context, final String pkg) {
		final DevicePolicies policies = new DevicePolicies(context);
		if (policies.invoke(DevicePolicyManager::isApplicationHidden, pkg)) {		// Hidden or not installed
			if (! policies.setApplicationHidden(pkg, false))
				if (! Apps.of(context).isInstalledInCurrentUser(pkg)) return "not_installed";	// Not installed in profile, just give up.
		}
		try {
			if (policies.isPackageSuspended(pkg))
				policies.invoke(DevicePolicyManager::setPackagesSuspended, new String[] { pkg }, false);
		} catch (final PackageManager.NameNotFoundException ignored) { return "not_found"; }
		return "";
	}

	@OwnerUser public static boolean launchApp(final Context context, final String pkg, final UserHandle profile) {
		final LauncherApps launcher_apps = requireNonNull((LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE));
		try {
			final List<LauncherActivityInfo> activities = launcher_apps.getActivityList(pkg, profile);
			if (activities == null || activities.isEmpty()) return false;
			launcher_apps.startMainActivity(activities.get(0).getComponentName(), profile, null, null);
		} catch (final SecurityException e) {		// SecurityException: Cannot retrieve activities for unrelated profile 10
			Log.e(TAG, "Error launching app: " + pkg + " @ user " + profile, e);
			return false;
		}
		return true;
	}

	public static @UserIdInt int[] getProfileIdsIncludingDisabled(final Context context) {
		if (Hacks.UserManager_getProfileIds != null)
			return Hacks.UserManager_getProfileIds.invoke(UserHandles.MY_USER_ID, false).on(requireNonNull(context.getSystemService(UserManager.class)));
		else return requireNonNull(context.getSystemService(UserManager.class)).getUserProfiles().stream().mapToInt(Users::toId).toArray();	// Fallback to profiles without disabled.
	}

	private static final String TAG = "Island.Manager";
}
