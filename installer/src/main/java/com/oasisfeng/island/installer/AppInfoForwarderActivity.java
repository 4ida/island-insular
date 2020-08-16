package com.oasisfeng.island.installer;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.oasisfeng.android.content.IntentCompat;
import com.oasisfeng.android.os.UserHandles;
import com.oasisfeng.android.util.Apps;
import com.oasisfeng.android.util.Suppliers;
import com.oasisfeng.island.shuttle.ActivityShuttle;
import com.oasisfeng.island.shuttle.Shuttle;
import com.oasisfeng.island.util.CallerAwareActivity;
import com.oasisfeng.island.util.Users;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import kotlin.Unit;
import kotlinx.coroutines.GlobalScope;

import static android.content.Intent.EXTRA_INITIAL_INTENTS;
import static android.content.Intent.FLAG_ACTIVITY_FORWARD_RESULT;
import static android.content.pm.PackageManager.MATCH_DEFAULT_ONLY;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.O;

/**
 * Created by Oasis on 2018-11-16.
 */
public class AppInfoForwarderActivity extends CallerAwareActivity {

	public static final String CALLER_PLACEHOLDER_FOR_SETTINGS = "settings";

	@Override protected void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Intent intent = getIntent().setComponent(null).setPackage(null);
		final UserHandle user = intent.getParcelableExtra(Intent.EXTRA_USER);
		if (user != null && Settings.ACTION_APPLICATION_DETAILS_SETTINGS.equals(intent.getAction())) {  // For profiles other than default
			intent.removeExtra(Intent.EXTRA_USER);
			new Shuttle(this, user).launch(GlobalScope.INSTANCE, true, context -> {
				context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return Unit.INSTANCE;
			});
			finish();
			return;
		}
		startActivity(buildTargetIntent(intent.getStringExtra(IntentCompat.EXTRA_PACKAGE_NAME), user, intent));
		finish();
	}

	private Intent buildTargetIntent(final String pkg, final @Nullable UserHandle user, final Intent target) {
		final Uri referrer = getReferrer(); final PackageManager pm = getPackageManager();
		final String referrer_pkg = referrer != null && "android-app".equals(referrer.getScheme()) ? referrer.getAuthority() : null;
		final String caller = getCallingPackage();
		Intent app_detail = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pkg, null));
		ResolveInfo app_detail_resolve = null;
		try {
			final List<ResolveInfo> app_detail_resolves = pm.queryIntentActivities(app_detail, PackageManager.MATCH_SYSTEM_ONLY);
			app_detail_resolve = app_detail_resolves.isEmpty() ? null : app_detail_resolves.get(0);
		} catch (final NullPointerException e) {	// Huawei-specific issue, only reported on Android 8, probably due to
			Log.e(TAG, "Error querying app detail activities: " + app_detail, e);
		}
		boolean caller_is_settings = false;
		final Supplier<List<ResolveInfo>> target_resolves = Suppliers.memoize(() -> pm.queryIntentActivities(target, MATCH_DEFAULT_ONLY/* Excluding this activity */));
		if (app_detail_resolve != null) {
			caller_is_settings = CALLER_PLACEHOLDER_FOR_SETTINGS.equals(referrer_pkg) || app_detail_resolve.activityInfo.packageName.equals(caller);
			if (! caller_is_settings && ! isCallerIslandButNotForwarder(caller)) {	// Started by 3rd-party app or this forwarder itself
				final Intent intent = new Intent(Intent.ACTION_SEARCH).putExtra(SearchManager.QUERY, "package:" + pkg).setPackage(getPackageName());
				if (user != null) intent.putExtra(Intent.EXTRA_USER, user);
				final ResolveInfo resolve = getPackageManager().resolveActivity(intent, 0);
				if (resolve != null) return intent.setClassName(this, resolve.activityInfo.name);
			}
			if (! caller_is_settings && user != null && ! UserHandles.MY_USER_HANDLE.equals(user)) {
				if (user.equals(Users.profile)) app_detail.setComponent(ActivityShuttle.getForwarder(this));	// Forwarding added in IslandProvisioning
				else app_detail.setComponent(getComponentName()).putExtra(Intent.EXTRA_USER, user);
			} else ActivityShuttle.forceNeverForwarding(pm, app_detail);

			if (SDK_INT < O && ! caller_is_settings && ! hasNonForwardingResolves(target_resolves.get()))
				return app_detail;		// Simulate EXTRA_AUTO_LAUNCH_SINGLE_CHOICE on Android pre-O.
		} else app_detail = null;

		final String title = getString(R.string.app_info_forwarder_title, Apps.of(this).getAppName(pkg), pkg);
		final Intent chooser = Intent.createChooser(target, title).addFlags(FLAG_ACTIVITY_FORWARD_RESULT);
		if (SDK_INT >= O) chooser.putExtra(IntentCompat.EXTRA_AUTO_LAUNCH_SINGLE_CHOICE, ! caller_is_settings);

		final List<Intent> initial_intents = new ArrayList<>();
		if (app_detail != null && ! caller_is_settings) {
			if (user != null && Users.isProfileManagedByIsland(user)) {	// Use mainland resolve to replace the misleading forwarding-resolved "Switch to work profile".
				final ActivityInfo activity = app_detail_resolve.activityInfo;
				final int labelRes = activity.labelRes != 0 ? activity.labelRes : activity.applicationInfo.labelRes;
				app_detail = new LabeledIntent(app_detail, activity.packageName, labelRes, activity.getIconResource());
			}
			initial_intents.add(app_detail);
		}
		// Also add app markets to the chooser. EXTRA_ALTERNATE_INTENTS is not used here due to inability of de-dup. (e.g. Google Play Store)
		final Intent market_intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg));
		final List<ResolveInfo> market_apps = getPackageManager().queryIntentActivities(market_intent, 0);
		final List<ComponentName> exclude_components = new ArrayList<>();
		market_apps.stream().map(r -> r.activityInfo).filter(ai -> ! ai.packageName.equals(caller)).forEachOrdered(market_activity -> {
			final Optional<ActivityInfo> dup_target = target_resolves.get().stream().map(r -> r.activityInfo).filter(target_activity ->
					market_activity.packageName.equals(target_activity.packageName) && market_activity.labelRes == target_activity.labelRes
					&& TextUtils.equals(market_activity.nonLocalizedLabel, target_activity.nonLocalizedLabel)).findFirst();
			if (dup_target.isPresent()) {
				final ActivityInfo dup_target_activity = dup_target.get();
				exclude_components.add(new ComponentName(dup_target_activity.packageName, dup_target_activity.name));
			}
			initial_intents.add(new Intent(market_intent).setClassName(market_activity.packageName, market_activity.name));
		});
		if (! caller_is_settings && isCallerIslandButNotForwarder(caller))
			exclude_components.add(new ComponentName(this, AppInfoForwarderActivity.class));
		if (! exclude_components.isEmpty()) chooser.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, exclude_components.toArray(new ComponentName[0]));
		if (! initial_intents.isEmpty()) chooser.putExtra(EXTRA_INITIAL_INTENTS, initial_intents.toArray(new Parcelable[0]));
		return chooser;
	}

	private boolean isCallerIslandButNotForwarder(final String caller) {
		return getPackageName().equals(caller) && (getIntent().getFlags() & FLAG_ACTIVITY_FORWARD_RESULT) == 0;
	}

	private static boolean hasNonForwardingResolves(final List<ResolveInfo> resolves) {
		return resolves != null && resolves.stream().anyMatch(candidate -> ! "android".equals(candidate.activityInfo.packageName));
	}

	private static final String TAG = "Island.AIFA";
}
