package com.oasisfeng.island.model;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.oasisfeng.android.app.Activities;
import com.oasisfeng.android.base.Scopes;
import com.oasisfeng.android.content.IntentCompat;
import com.oasisfeng.android.databinding.recyclerview.ItemBinder;
import com.oasisfeng.android.ui.Dialogs;
import com.oasisfeng.android.widget.Toasts;
import com.oasisfeng.androidx.lifecycle.NonNullMutableLiveData;
import com.oasisfeng.common.app.BaseAppListViewModel;
import com.oasisfeng.island.analytics.Analytics;
import com.oasisfeng.island.controller.IslandAppClones;
import com.oasisfeng.island.data.IslandAppInfo;
import com.oasisfeng.island.data.IslandAppListProvider;
import com.oasisfeng.island.data.helper.AppStateTrackingHelper;
import com.oasisfeng.island.engine.IslandManager;
import com.oasisfeng.island.featured.FeaturedListViewModel;
import com.oasisfeng.island.greenify.GreenifyClient;
import com.oasisfeng.island.mobile.BR;
import com.oasisfeng.island.mobile.R;
import com.oasisfeng.island.shortcut.AbstractAppLaunchShortcut;
import com.oasisfeng.island.shuttle.MethodShuttle;
import com.oasisfeng.island.util.DevicePolicies;
import com.oasisfeng.island.util.OwnerUser;
import com.oasisfeng.island.util.ProfileUser;
import com.oasisfeng.island.util.Users;

import java.util.Collection;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.HandlerCompat;
import androidx.lifecycle.MutableLiveData;
import java9.util.concurrent.CompletableFuture;
import java9.util.concurrent.CompletionStage;
import java9.util.function.BooleanSupplier;
import java9.util.function.Predicate;
import java9.util.stream.Collectors;
import java9.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;

/**
 * View model for apps
 *
 * Created by Oasis on 2015/7/7.
 */
@ParametersAreNonnullByDefault
public class AppListViewModel extends BaseAppListViewModel<AppViewModel> {

	private static final long QUERY_TEXT_DELAY = 300;	// The delay before typed query text is applied
	private static final String STATE_KEY_FILTER_PRIMARY_CHOICE = "filter.primary";
	private static final String STATE_KEY_FILTER_HIDDEN_SYSTEM_APPS = "filter.hidden_sys";

	/** Workaround for menu res reference not supported by data binding */ public static @MenuRes int actions_menu = R.menu.app_actions;

	public enum Filter {
		// Name		Visibility			Filter
		Island		(Users::hasProfile,	app -> Users.isProfileManagedByIsland(app.user) && app.shouldShowAsEnabled() && app.isInstalled()),
		Mainland	(() -> true,		app -> Users.isOwner(app.user) && (app.isSystem() || app.isInstalled())),	// Including uninstalled system app
		;
		boolean available() { return mVisibility.getAsBoolean(); }
		Filter(final BooleanSupplier visibility, final Predicate<IslandAppInfo> filter) { mVisibility = visibility; mFilter = filter; }

		private final BooleanSupplier mVisibility;
		private final Predicate<IslandAppInfo> mFilter;
	}

	private Predicate<IslandAppInfo> activeFilters() {
		return mActiveFilters;
	}

	/** @see SearchView.OnQueryTextListener#onQueryTextChange(String) */
	public boolean onQueryTextChange(final String text) {
		mHandler.removeCallbacksAndMessages(mFilterText);		// In case like "A -> AB -> A" in a short time
		if (TextUtils.equals(text, mFilterText.getValue())) return true;
		HandlerCompat.postDelayed(mHandler, () -> mFilterText.setValue(text), mFilterText, QUERY_TEXT_DELAY);	// A short delay to avoid flickering during typing.
		return true;
	}

	/** @see SearchView.OnQueryTextListener#onQueryTextSubmit(String) */
	public void onQueryTextSubmit(final String text) {
		mChipsVisible.setValue(! TextUtils.isEmpty(text) || mFilterIncludeHiddenSystemApps.getValue());
		if (! TextUtils.equals(text, mFilterText.getValue())) {
			mHandler.removeCallbacksAndMessages(mFilterText);
			mFilterText.setValue(text);
		}
	}

	public void onSearchClick(final SearchView view) {
		view.setQuery(mFilterText.getValue(), false);
	}

	/** @see SearchView.OnCloseListener#onClose() */
	public boolean onSearchViewClose() {
		onQueryTextSubmit(mFilterText.getValue());
		return true;
	}

	public void onQueryTextCleared() {
		onQueryTextSubmit("");
	}

	private void updateActiveFilters() {
		if (mFilterShared == null) return;		// When called by constructor
		final Filter primary_filter = mPrimaryFilter.getValue();
		if (primary_filter == null) return;
		Log.d(TAG, "Primary filter: " + primary_filter);
		Predicate<IslandAppInfo> filters = mFilterShared.and(primary_filter.mFilter);
		if (! mFilterIncludeHiddenSystemApps.getValue())
			filters = filters.and(app -> ! app.isSystem() || app.isInstalled() && app.isLaunchable());
		final String text = mFilterText.getValue();
		if (! TextUtils.isEmpty(text)) {
			if (text.startsWith("package:")) {
				final String pkg = text.substring(8);
				filters = filters.and(app -> app.packageName.equals(pkg));
			} else {    // TODO: Support T9 Pinyin
				final String text_lc = text.toLowerCase();
				filters = filters.and(app -> app.packageName.toLowerCase().contains(text_lc) || app.getLabel().toLowerCase().contains(text_lc));
			}
		}
		mActiveFilters = filters;

		final AppViewModel selected = mSelection.getValue();
		clearSelection();

		IslandAppInfo.cacheLaunchableApps(requireNonNull(mAppListProvider.getContext()));	// Performance optimization
		final List<AppViewModel> apps = mAppListProvider.installedApps().filter(activeFilters()).map(AppViewModel::new).collect(Collectors.toList());
		IslandAppInfo.invalidateLaunchableAppsCache();
		replaceApps(apps);

		if (selected != null) for (final AppViewModel app : apps)
			if (app.info().packageName.equals(selected.info().packageName)) {
				setSelection(app);
				break;
			}
	}

	public AppListViewModel() {
		super(AppViewModel.class);
		mSelection.observeForever(selection -> updateActions());
	}

	public boolean onTabSwitched(final Context context, final MenuItem tab) {
		setTitle(context, tab);
		final int tab_menu_id = tab.getItemId();
		if (tab_menu_id == R.id.tab_discovery) {
			mPrimaryFilter.setValue(null);
			mFeatured.visible.setValue(Boolean.TRUE);
			mFeatured.update(context);
			return true;
		} else mFeatured.visible.setValue(Boolean.FALSE);

		if (tab_menu_id == R.id.tab_island) {
			if (! Filter.Island.available()) return false;
			mPrimaryFilter.setValue(Filter.Island);
		} else if (tab_menu_id == R.id.tab_mainland) {
			mPrimaryFilter.setValue(Filter.Mainland);
		} else return false;
		return true;
	}

	public void attach(final Context context, final Menu actions, final BottomNavigationView tabs, final @Nullable Bundle state) {
		mAppListProvider = IslandAppListProvider.getInstance(context);
		mDeviceOwner = new DevicePolicies(context).isActiveDeviceOwner();
		mActions = actions;
		mFilterShared = IslandAppListProvider.excludeSelf(context);

		if (! Filter.Island.available()) {		// Island is unavailable
			tabs.getMenu().removeItem(R.id.tab_island);
			tabs.setSelectedItemId(R.id.tab_mainland);
			mPrimaryFilter.setValue(Filter.Mainland);
			setTitle(context, tabs.getMenu().findItem(R.id.tab_mainland));
		} else {
			Filter primary_filter = Filter.Island;		// Default
			if (state != null) {
				final int ordinal = state.getInt(STATE_KEY_FILTER_PRIMARY_CHOICE, -1);
				if (ordinal >= 0 && ordinal < Filter.values().length) primary_filter = Filter.values()[ordinal];
				else {
					UserHandle user = state.getParcelable(Intent.EXTRA_USER);
					if (user == null) user = Users.current();
					if (user.equals(Users.owner)) primary_filter = Filter.Mainland;
				}
			}
			tabs.setSelectedItemId(primary_filter == Filter.Mainland ? R.id.tab_mainland : R.id.tab_island);
			mPrimaryFilter.setValue(primary_filter);
			mFilterIncludeHiddenSystemApps.setValue(state != null && state.getBoolean(STATE_KEY_FILTER_HIDDEN_SYSTEM_APPS));
			setTitle(context, tabs.getMenu().findItem(tabs.getSelectedItemId()));
		}
		if (state != null) {
			final String filter_text = state.getString(SearchManager.QUERY);
			if (filter_text != null && ! filter_text.isEmpty()) onQueryTextSubmit(filter_text);
		}
		// Start observation after initial value is set.
		mPrimaryFilter.observeForever(filter -> updateActiveFilters());
		mFilterIncludeHiddenSystemApps.observeForever(filter -> updateActiveFilters());
		mFilterText.observeForever(text -> updateActiveFilters());
		mSelection.observeForever(selection -> {
			final Interpolator interpolator = new AccelerateDecelerateInterpolator();
			if (selection != null) tabs.animate().alpha(0).translationZ(-10).scaleX(0.95f).scaleY(0.95f).setDuration(200).setInterpolator(interpolator);
			else tabs.animate().alpha(1).translationZ(0).scaleX(1).scaleY(1).setDuration(200).setInterpolator(interpolator);
		});
	}

	private static void setTitle(final Context context, final MenuItem tab) {
		if (context instanceof Activity) requireNonNull(((Activity) context).getActionBar()).setTitle(tab.getTitle());
	}

	public void onSaveInstanceState(final Bundle saved) {
		final Filter primary_filter = mPrimaryFilter.getValue();
		if (primary_filter != null) saved.putInt(STATE_KEY_FILTER_PRIMARY_CHOICE, primary_filter.ordinal());
		saved.putBoolean(STATE_KEY_FILTER_HIDDEN_SYSTEM_APPS, mFilterIncludeHiddenSystemApps.getValue());
		final String text = mFilterText.getValue();
		if (! text.isEmpty()) saved.putString(SearchManager.QUERY, text);
	}

	private void updateActions() {
		final AppViewModel selection = mSelection.getValue();
		if (selection == null) return;
		final IslandAppInfo app = selection.info();
		Analytics.$().trace("app", app.packageName).trace("user", Users.toId(app.user)).trace("hidden", app.isHidden())
				.trace("system", app.isSystem()).trace("critical", app.isCritical());
		final UserHandle profile = Users.profile;
		final boolean exclusive = mAppListProvider.isExclusive(app);

		final boolean system = app.isSystem(), installed = app.isInstalled(), in_owner = Users.isOwner(app.user), is_managed = mDeviceOwner || ! in_owner;
		mActions.findItem(R.id.menu_freeze).setVisible(installed && is_managed && ! app.isHidden() && app.enabled);
		mActions.findItem(R.id.menu_unfreeze).setVisible(installed && is_managed && app.isHidden());
		mActions.findItem(R.id.menu_clone).setVisible(in_owner && profile != null && exclusive);
		mActions.findItem(R.id.menu_clone_back).setVisible(! in_owner && exclusive);
		mActions.findItem(R.id.menu_reinstall).setVisible(! installed);
		mActions.findItem(R.id.menu_remove).setVisible(installed && (exclusive ? system : (! system || app.shouldShowAsEnabled())));	// Disabled system app is treated as "removed".
		mActions.findItem(R.id.menu_uninstall).setVisible(installed && exclusive && ! system);	// "Uninstall" for exclusive user app, "Remove" for exclusive system app.
		mActions.findItem(R.id.menu_shortcut).setVisible(installed && is_managed && app.isLaunchable() && app.enabled);
		mActions.findItem(R.id.menu_greenify).setVisible(installed && is_managed && app.enabled);
	}

	public void onPackagesUpdate(final Collection<IslandAppInfo> apps) {
		final Predicate<IslandAppInfo> filters = activeFilters();
		for (final IslandAppInfo app : apps)
			if (filters.test(app)) {
				putApp(app.packageName, new AppViewModel(app));
			} else removeApp(app.packageName, app.user);
		updateActions();
	}

	private void removeApp(final String pkg, final UserHandle user) {
		final AppViewModel app = getApp(pkg);
		if (app != null && app.info().user.equals(user)) super.removeApp(pkg);
	}

	public void onPackagesRemoved(final Collection<IslandAppInfo> apps) {
		final Predicate<IslandAppInfo> filters = activeFilters();
		for (final IslandAppInfo app : apps)
			if (filters.test(app)) removeApp(app.packageName);
		updateActions();
	}

	public final void onItemLaunchIconClick(final Context context, final IslandAppInfo app) {
		if (mSelection.getValue() == null) return;
		final String pkg = app.packageName;
		
		if (! app.isHidden()) {		// Not frozen, launch the app directly. TODO: If isBlocked() ?
			if (! IslandManager.launchApp(context, pkg, app.user))
				Toast.makeText(context, context.getString(R.string.toast_app_launch_failure, app.getLabel()), Toast.LENGTH_SHORT).show();
		} else {
			(Users.isOwner(app.user) ? CompletableFuture.completedFuture(IslandManager.ensureAppFreeToLaunch(context, pkg))
					: MethodShuttle.runInProfile(context, () -> IslandManager.ensureAppFreeToLaunch(context, pkg))).thenAccept(failure -> {
				if (failure == null)
					if (! IslandManager.launchApp(context, pkg, app.user)) failure = "launcher_activity_not_found";
				if (failure != null) {
					Toast.makeText(context, R.string.toast_failed_to_launch_app, Toast.LENGTH_LONG).show();
					
				}
			}).exceptionally(t -> {
				reportAndShowToastForInternalException(context, "Error unfreezing and launching app: " + pkg, t);
				return null;
			});
		}
	}

	public boolean onActionClick(final Context context, final MenuItem item) {
		final AppViewModel selection = mSelection.getValue();
		if (selection == null) return false;
		final IslandAppInfo app = selection.info();
		final String pkg = app.packageName;

		final int id = item.getItemId();
		if (id == R.id.menu_clone) {
			final IslandAppInfo app_in_profile = IslandAppListProvider.getInstance(context).get(pkg, Users.profile);
			if (app_in_profile != null && app_in_profile.isHiddenSysIslandAppTreatedAsDisabled()) {	// Frozen system app shown as disabled, just unfreeze it.
				MethodShuttle.runInProfile(context, () -> IslandManager.ensureAppHiddenState(context, pkg, false)).thenAccept(unfrozen -> {
					if (unfrozen) {
						app.stopTreatingHiddenSysAppAsDisabled();
						Toast.makeText(context, context.getString(R.string.toast_successfully_cloned, app.getLabel()), Toast.LENGTH_SHORT).show();
					}
				}).exceptionally(t -> { reportAndShowToastForInternalException(context, "Error unfreezing app: " + pkg, t); return null; });
			} else if (app_in_profile != null && app_in_profile.isInstalled() && ! app_in_profile.enabled) {	// Disabled system app is shown as "removed" (not cloned)
				launchSystemAppSettings(context, app_in_profile);
				Toast.makeText(context, R.string.toast_enable_disabled_system_app, Toast.LENGTH_SHORT).show();
			} else IslandAppClones.cloneApp(context, app);
			clearSelection();
		} else if (id == R.id.menu_clone_back) {
			Activities.startActivity(context, new Intent(Intent.ACTION_INSTALL_PACKAGE, Uri.fromParts("package", pkg, null)));
			
			clearSelection();
		} else if (id == R.id.menu_freeze) {// Select the next alive app, or clear selection.
			

			final Activity activity = Activities.findActivityFrom(context);
			if (activity != null && IslandAppListProvider.getInstance(context).isCritical(pkg)) {
				Dialogs.buildAlert(activity, R.string.dialog_title_warning, R.string.dialog_critical_app_warning)
						.withCancelButton().withOkButton(() -> freezeApp(context, selection)).show();
			} else freezeApp(context, selection);
		} else if (id == R.id.menu_unfreeze) {
			
			unfreeze(context, app).thenAccept(result -> {
				if (! result) return;
				refreshAppStateAsSysBugWorkaround(context, pkg);
				clearSelection();
			});
		} else if (id == R.id.menu_app_settings) {
			launchExternalAppSettings(context, app);
		} else if (id == R.id.menu_remove || id == R.id.menu_uninstall) {
			onRemovalRequested(context);
		} else if (id == R.id.menu_reinstall) {
			onReinstallRequested(context);
		} else if (id == R.id.menu_shortcut) {
			onShortcutRequested(context);
		} else if (id == R.id.menu_greenify) {
			onGreenifyRequested(context);
//		} else if (id == R.id.menu_enable) {
//			final LauncherApps launcher_apps = (LauncherApps) mActivity.getSystemService(Context.LAUNCHER_APPS_SERVICE);
//			launcher_apps.startAppDetailsActivity(new ComponentName(pkg, ""), selection.info().user, null, null);
		}
		return true;
	}

	private void freezeApp(final Context context, final AppViewModel app_vm) {
		final IslandAppInfo app = app_vm.info();
		final String pkg = app.packageName;
		(Users.isOwner(app.user) ? CompletableFuture.completedFuture(ensureAppHiddenState(context, pkg, true))
				: MethodShuttle.runInProfile(context, () -> ensureAppHiddenState(context, pkg, true))).thenAccept(frozen -> {
			if (frozen) {
				app.stopTreatingHiddenSysAppAsDisabled();
				// Select the next app for convenient continuous freezing.
				final int next_index = indexOf(app_vm) + 1;
				final AppViewModel next;
				if (next_index < size() && (next = getAppAt(next_index)).state == AppViewModel.State.Alive) setSelection(next);
				else clearSelection();
			}
			refreshAppStateAsSysBugWorkaround(context, pkg);
		});
	}

	@OwnerUser @ProfileUser private static boolean ensureAppHiddenState(final Context context, final String pkg, final boolean hidden) {
		final DevicePolicies policies = new DevicePolicies(context);
		if (policies.setApplicationHidden(pkg, hidden)) return true;
		// Since setApplicationHidden() return false if already in that state, also check the current state.
		final boolean state = policies.invoke(DevicePolicyManager::isApplicationHidden, pkg);
		if (hidden == state) return true;
		final List<ComponentName> active_admins = policies.getManager().getActiveAdmins();
		if (active_admins != null && StreamSupport.stream(active_admins).anyMatch(admin -> pkg.equals(admin.getPackageName())))
			Toasts.showLong(context, R.string.toast_error_freezing_active_admin);		// TODO: Action to open device-admin settings.
		else Toasts.showLong(context, R.string.toast_error_freeze_failure);
		return false;
	}

	private static void launchSystemAppSettings(final Context context, final IslandAppInfo app) {
		// Stock app info activity requires the target app not hidden.
		unfreezeIfNeeded(context, app).thenAccept(unfrozen -> {
			if (unfrozen) requireNonNull((LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE))
					.startAppDetailsActivity(new ComponentName(app.packageName, ""), app.user, null, null);
		});
	}

	private static void launchExternalAppSettings(final Context context, final IslandAppInfo app) {
		final Intent intent = new Intent(IntentCompat.ACTION_SHOW_APP_INFO).setPackage(context.getPackageName())
				.putExtra(IntentCompat.EXTRA_PACKAGE_NAME, app.packageName).putExtra(Intent.EXTRA_USER, app.user);
		final ResolveInfo resolve = context.getPackageManager().resolveActivity(intent, 0);
		if (resolve == null) return;		// Should never happen as module "installer" is always bundled with "mobile".
		intent.setComponent(new ComponentName(resolve.activityInfo.packageName, resolve.activityInfo.name));
		unfreezeIfNeeded(context, app).thenAccept(unfrozen -> Activities.startActivity(context, intent));
	}

	private static CompletionStage<Boolean> unfreezeIfNeeded(final Context context, final IslandAppInfo app) {
		return app.isHidden() ? unfreeze(context, app) : CompletableFuture.completedFuture(true);
	}

	private void onShortcutRequested(final Context context) {
		final AppViewModel app_vm = mSelection.getValue();
		if (app_vm == null) return;
		final IslandAppInfo app= app_vm.info();
		final String pkg = app.packageName;
		
		final String shortcut_prefix = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_launch_shortcut_prefix), context.getString(R.string.default_launch_shortcut_prefix));
		final Boolean result = AbstractAppLaunchShortcut.createOnLauncher(context, pkg, app, app.user, shortcut_prefix + app.getLabel(), app.icon);
		if (result == null || result) Toast.makeText(context, R.string.toast_shortcut_request_sent, Toast.LENGTH_SHORT).show();	// MIUI has no UI for shortcut pinning.
		else Toast.makeText(context, R.string.toast_shortcut_failed, Toast.LENGTH_LONG).show();
	}

	private void onGreenifyRequested(final Context context) {
		if (mSelection.getValue() == null) return;
		final IslandAppInfo app = mSelection.getValue().info();
		

		final String mark = "greenify-explained";
		final Boolean greenify_ready = GreenifyClient.checkGreenifyVersion(context);
		final boolean greenify_installed = greenify_ready != null;
		final boolean unavailable_or_version_too_low = greenify_ready == null || ! greenify_ready;
		if (unavailable_or_version_too_low || ! Scopes.app(context).isMarked(mark)) {
			String message = context.getString(R.string.dialog_greenify_explanation);
			if (greenify_installed && unavailable_or_version_too_low)
				message += "\n\n" + context.getString(R.string.dialog_greenify_version_too_low);
			final int button = ! greenify_installed ? R.string.action_install : ! greenify_ready ? R.string.action_upgrade : R.string.action_continue;
			new AlertDialog.Builder(context).setTitle(R.string.dialog_greenify_title).setMessage(message).setPositiveButton(button, (d, w) -> {
				if (! unavailable_or_version_too_low) {
					Scopes.app(context).markOnly(mark);
					greenify(context, app);
				} else GreenifyClient.openInAppMarket(context);
			}).show();
		} else greenify(context, app);
	}

	private static void greenify(final Context context, final IslandAppInfo app) {
		if (! GreenifyClient.greenify(context, app.packageName, app.user))
			Toast.makeText(context, R.string.toast_greenify_failed, Toast.LENGTH_LONG).show();
	}

	private void onRemovalRequested(final Context context) {
		if (mSelection.getValue() == null) return;
		final IslandAppInfo app = mSelection.getValue().info();
		
		unfreezeIfNeeded(context, app).thenAccept(unfrozen -> {
			if (app.isSystem()) {
				
				final Activity activity = requireNonNull(Activities.findActivityFrom(context));
				if (app.isCritical()) {
					Dialogs.buildAlert(activity, R.string.dialog_title_warning, R.string.dialog_critical_app_warning).withCancelButton()
							.setPositiveButton(R.string.action_continue, (d, w) -> launchSystemAppSettings(context, app)).show();
				} else Dialogs.buildAlert(activity, 0, R.string.prompt_disable_sys_app_as_removal).withCancelButton()
						.setPositiveButton(R.string.action_continue, (d, w) -> launchSystemAppSettings(context, app)).show();
			} else {
				Activities.startActivity(context, new Intent(Intent.ACTION_UNINSTALL_PACKAGE)
						.setData(Uri.fromParts("package", app.packageName, null)).putExtra(Intent.EXTRA_USER, app.user));
				if (! Users.isProfileRunning(context, app.user)) {	// App clone can actually be removed in quiet mode, without callback triggered.
					final Activity activity = Activities.findActivityFrom(context);
					if (activity != null) AppStateTrackingHelper.requestSyncWhenResumed(activity, app.packageName, app.user);
				}
			}
		});
	}

	private void onReinstallRequested(final Context context) {
		if (mSelection.getValue() == null) return;
		final IslandAppInfo app = mSelection.getValue().info();
		final Activity activity = Activities.findActivityFrom(context);
		if (activity == null) reinstallSystemApp(context, app);
		else Dialogs.buildAlert(activity, R.string.dialog_title_warning, R.string.dialog_reinstall_system_app_warning)
				.withCancelButton().setPositiveButton(R.string.action_continue, (d, w) -> reinstallSystemApp(context, app)).show();
	}

	private static void reinstallSystemApp(final Context context, final IslandAppInfo app) {
		final DevicePolicies policies = new DevicePolicies(context);
		policies.execute(DevicePolicyManager::enableSystemApp, app.packageName);
	}

	/** Possible 10s delay before the change broadcast could be received (due to Android issue 225880), so we force a refresh immediately. */
	private static void refreshAppStateAsSysBugWorkaround(final Context context, final String pkg) {
		IslandAppListProvider.getInstance(context).refreshPackage(pkg, Users.profile, false);
	}

	private static CompletionStage<Boolean> unfreeze(final Context context, final IslandAppInfo app) {
		final String pkg = app.packageName;
		return Users.isOwner(app.user) ? CompletableFuture.completedFuture(ensureAppHiddenState(context, pkg, false))
				: MethodShuttle.runInProfile(context, () -> ensureAppHiddenState(context, pkg, false)).exceptionally(t -> {
			reportAndShowToastForInternalException(context, "Error unfreezing app: " + app.packageName, t);
			return false;
		});
	}

	public final void onItemClick(final AppViewModel clicked) {
		setSelection(clicked != mSelection.getValue() ? clicked : null);	// Click the selected one to deselect
	}

	@SuppressWarnings("MethodMayBeStatic") public final void onBottomSheetClick(final View view) {
		final BottomSheetBehavior bottom_sheet = BottomSheetBehavior.from(view);
		bottom_sheet.setState(BottomSheetBehavior.STATE_EXPANDED);
	}

	private static void reportAndShowToastForInternalException(final Context context, final String log, final Throwable t) {
		Analytics.$().logAndReport(TAG, log, t);
		Toast.makeText(context, "Internal error: " + t.getMessage(), Toast.LENGTH_LONG).show();
	}

	/* Parcelable */

	public final BottomSheetBehavior.BottomSheetCallback bottom_sheet_callback = new BottomSheetBehavior.BottomSheetCallback() {

		@Override public void onStateChanged(@NonNull final View bottom_sheet, final int new_state) {
			if (new_state == BottomSheetBehavior.STATE_HIDDEN) clearSelection();
			else bottom_sheet.bringToFront();	// Force a lift due to bottom sheet appearing underneath BottomNavigationView on some devices, despite the layout order or elevation.
		}

		@Override public void onSlide(@NonNull final View bottomSheet, final float slideOffset) {}
	};

	public final ItemBinder<AppViewModel> item_binder = (container, model, item) -> {
		item.setVariable(BR.app, model);
		item.setVariable(BR.apps, this);
	};

	public FeaturedListViewModel mFeatured;
	/* Attachable fields */
	private IslandAppListProvider mAppListProvider;
	private Menu mActions;
	/* Parcelable fields */
	public final MutableLiveData<Filter> mPrimaryFilter = new MutableLiveData<>();
	public final NonNullMutableLiveData<Boolean> mFilterIncludeHiddenSystemApps = new NonNullMutableLiveData<>(false);
	public final NonNullMutableLiveData<String> mFilterText = new NonNullMutableLiveData<>("");
	/* Transient fields */
	public final NonNullMutableLiveData<Boolean> mChipsVisible = new NonNullMutableLiveData<>(false);
	private Predicate<IslandAppInfo> mFilterShared;			// All other filters to apply always
	private boolean mDeviceOwner;
	private Predicate<IslandAppInfo> mActiveFilters;		// The active composite filters
	private final Handler mHandler = new Handler();

	private static final String TAG = "Island.Apps";
}
