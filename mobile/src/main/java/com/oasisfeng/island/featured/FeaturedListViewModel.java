package com.oasisfeng.island.featured;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.oasisfeng.android.app.Activities;
import com.oasisfeng.android.base.Scopes;
import com.oasisfeng.android.databinding.ObservableSortedList;
import com.oasisfeng.android.databinding.recyclerview.BindingRecyclerViewAdapter;
import com.oasisfeng.android.databinding.recyclerview.ItemBinder;
import com.oasisfeng.android.ui.WebContent;
import com.oasisfeng.android.util.Apps;
import com.oasisfeng.android.util.SafeAsyncTask;
import com.oasisfeng.androidx.lifecycle.NonNullMutableLiveData;
import com.oasisfeng.island.Config;
import com.oasisfeng.island.adb.AdbSecure;
import com.oasisfeng.island.analytics.Analytics;
import com.oasisfeng.island.data.LiveUserRestriction;
import com.oasisfeng.island.files.IslandFiles;
import com.oasisfeng.island.mobile.BR;
import com.oasisfeng.island.mobile.R;
import com.oasisfeng.island.mobile.databinding.FeaturedEntryBinding;
import com.oasisfeng.island.settings.IslandSettingsFragment;
import com.oasisfeng.island.settings.SettingsActivity;
import com.oasisfeng.island.setup.IslandSetup;
import com.oasisfeng.island.setup.SetupActivity;
import com.oasisfeng.island.setup.SetupViewModel;
import com.oasisfeng.island.util.DevicePolicies;
import com.oasisfeng.island.util.Permissions;
import com.oasisfeng.island.util.Users;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import eu.chainfire.libsuperuser.Shell;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.UserManager.DISALLOW_DEBUGGING_FEATURES;
import static androidx.lifecycle.Transformations.map;
import static androidx.recyclerview.widget.ItemTouchHelper.END;
import static androidx.recyclerview.widget.ItemTouchHelper.START;

/**
 * View-model for featured list
 *
 * Created by Oasis on 2018/5/18.
 */
@ParametersAreNonnullByDefault
public class FeaturedListViewModel extends AndroidViewModel {

	private static final String SCOPE_TAG_PREFIX_FEATURED = "featured_";
	private static final boolean SHOW_ALL = false;		// For debugging purpose

	public NonNullMutableLiveData<Boolean> visible = new NonNullMutableLiveData<>(Boolean.FALSE);
	public final ObservableSortedList<FeaturedViewModel> features = new ObservableSortedList<>(FeaturedViewModel.class);

	public final ItemBinder<FeaturedViewModel> item_binder = (container, model, binding) -> binding.setVariable(BR.vm, model);

	public final ItemTouchHelper item_touch_helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, START | END) {

		@Override public void onSwiped(final RecyclerView.ViewHolder holder, final int direction) {
			final FeaturedViewModel vm = ((FeaturedEntryBinding) ((BindingRecyclerViewAdapter.ViewHolder) holder).binding).getVm();
			final int index = features.indexOf(vm);
			final boolean mark_read = direction != START || ! vm.dismissed.getValue();		// Left-swipe to mark-unread for already read entry
			vm.dismissed.setValue(mark_read);
			features.updateItemAt(index, vm);
			final Scopes.Scope app_scope = Scopes.app(holder.itemView.getContext());
			final String scope_tag = SCOPE_TAG_PREFIX_FEATURED + vm.tag;
			if (mark_read) app_scope.markOnly(scope_tag);
			else app_scope.unmark(scope_tag);
		}

		@Override public boolean onMove(final RecyclerView view, final RecyclerView.ViewHolder vh, final RecyclerView.ViewHolder vht) { return false; }
	});

	public void update(final FragmentActivity activity) {
		final Application app = getApplication();
		final DevicePolicies policies = new DevicePolicies(activity);
		final boolean is_mainland_owner = policies.isProfileOrDeviceOwnerOnCallingUser(), has_profile = Users.hasProfile();
		features.beginBatchedUpdates();
		features.clear();

		if (SHOW_ALL || IslandFiles.isCompatible(activity)) {
			final boolean has_across_users_permission = Permissions.has(activity, Permissions.INTERACT_ACROSS_USERS);
			if (! has_across_users_permission)
				addFeature(app, "file_shuttle_prereq", R.string.featured_file_shuttle_title, R.string.featured_file_shuttle_description, 0,
						R.string.action_learn_more, c -> WebContent.view(c, Config.URL_FILE_SHUTTLE.get()));
			else if (! Permissions.has(activity, WRITE_EXTERNAL_STORAGE) || ! IslandFiles.isFileShuttleEnabled(activity)) {
				final String tag = "file_shuttle";
				addFeatureRaw(app, tag, R.string.featured_file_shuttle_title, R.string.featured_file_shuttle_description,
						0, R.string.action_activate, vm -> { if (IslandFiles.enableFileShuttle(activity)) removeFeature(tag); });
			} else {
				Analytics.$().setProperty(Analytics.Property.FileShuttleEnabled, "1");
				addFeaturedApp(R.string.featured_fx_title, R.string.featured_fx_description, R.drawable.ic_launcher_fx, "nextapp.fx");
			}
		}

		final boolean dev_enabled = "1".equals(Settings.Global.getString(app.getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED));
		final LiveUserRestriction adb_secure = ! is_mainland_owner && ! has_profile ? null
				: new LiveUserRestriction(app, DISALLOW_DEBUGGING_FEATURES, is_mainland_owner ? Users.owner : Users.profile);
		if (adb_secure != null && (SHOW_ALL || dev_enabled || adb_secure.query(activity))) {	// ADB is disabled so long as ADB secure is enabled.
			addFeatureRaw(app, "adb_secure", is_mainland_owner ? R.string.featured_adb_secure_title : R.string.featured_adb_secure_island_title,
					R.string.featured_adb_secure_description,0, map(adb_secure, enabled -> enabled ? R.string.action_disable : R.string.action_enable),
					vm -> AdbSecure.toggleAdbSecure(activity, Objects.equals(vm.button.getValue(), R.string.action_enable), false));
		}

		if (SHOW_ALL || is_mainland_owner && ! Users.hasProfile() && policies.isActiveDeviceOwner())    // New profile can not be setup if owner user is in managed profile mode.
			addFeature(app, "setup_island", R.string.featured_setup_island_title, R.string.setup_island_intro, 0, R.string.featured_button_setup, c -> {
				if (SetupViewModel.checkManagedProvisioningPrerequisites(c, true) == null) {
					startSetupActivityCleanly(c);		// Prefer ManagedProvision, which could also fallback to root routine.
				} else SafeAsyncTask.execute(activity, a -> Shell.SU.available(), (cc, su_available) -> {
					if (su_available) IslandSetup.requestProfileOwnerSetupWithRoot(activity);
					else WebContent.view(cc, Uri.parse(Config.URL_SETUP.get()));
				});
			});

		if (SHOW_ALL || ! is_mainland_owner)
			addFeature(app, "managed_mainland", R.string.featured_managed_mainland_title, R.string.featured_managed_mainland_description, 0,
					R.string.featured_button_setup, c -> SettingsActivity.startWithPreference(activity, IslandSettingsFragment.class));

		addFeaturedApp(R.string.featured_greenify_title, R.string.featured_greenify_description, R.drawable.ic_launcher_greenify, "com.oasisfeng.greenify");
		addFeaturedApp(R.string.featured_saf_enhancer_title, R.string.featured_saf_enhancer_description, R.drawable.ic_launcher_saf_enhancer,
				"app.gwo.safenhancer.lite", "app.gwo.safenhancer");

		addFeaturedApp(R.string.featured_appops_title, R.string.featured_appops_description, R.drawable.ic_launcher_appops,
				"rikka.appops", "rikka.appops.pro");

		features.endBatchedUpdates();
	}

	private static void startSetupActivityCleanly(final Context context) {
		// Finish all tasks of Island first to avoid state inconsistency.
		final ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		if (am != null) {
			final List<ActivityManager.AppTask> tasks = am.getAppTasks();
			if (tasks != null) for (final ActivityManager.AppTask task : tasks) task.finishAndRemoveTask();
		}
		Activities.startActivity(context, new Intent(context, SetupActivity.class));
	}

	private boolean addFeaturedApp(final @StringRes int title, final @StringRes int description, final @DrawableRes int icon, final String... pkgs) {
		if (! SHOW_ALL) for (final String pkg : pkgs) if (mApps.isInstalledInCurrentUser(pkg)) return false;
		final String pkg = pkgs[0];
		addFeature(getApplication(), pkg, title, description, icon, R.string.action_install, c -> showInMarket(c, pkg));
		return true;
	}

	private static void showInMarket(final Context context, final String pkg) {
		Analytics.$().event("featured_install").with(Analytics.Param.ITEM_ID, pkg).send();
		Apps.of(context).showInMarket(pkg, "island", "featured");
	}

	private void addFeature(final Application app, final String tag, final @StringRes int title, final @StringRes int description,
							final @DrawableRes int icon, final @StringRes int button, final Consumer<Context> function) {
		addFeatureRaw(app, tag, title, description, icon, button, vm -> function.accept(vm.getApplication()));
	}

	private void addFeatureRaw(final Application app, final String tag, final @StringRes int title, final @StringRes int description,
							   final @DrawableRes int icon, final @StringRes int button, final Consumer<FeaturedViewModel> function) {
		addFeatureRaw(app, tag, title, description, icon, new NonNullMutableLiveData<>(button), function);
	}

	private void addFeatureRaw(final Application app, final String tag, final @StringRes int title, final @StringRes int description,
							   final @DrawableRes int icon, final LiveData<Integer> button, final Consumer<FeaturedViewModel> function) {
		features.add(new FeaturedViewModel(app, sOrderGenerator.incrementAndGet(), tag, app.getString(title), app.getText(description),
				icon != 0 ? app.getDrawable(icon) : null, button, function, Scopes.app(app).isMarked(SCOPE_TAG_PREFIX_FEATURED + tag)));
	}

	private void removeFeature(final String tag) {
		for (final Iterator<FeaturedViewModel> iterator = features.iterator(); iterator.hasNext(); ) {
			if (! tag.equals(iterator.next().tag)) continue;
			iterator.remove();
			return;
		}
	}

	public FeaturedListViewModel(final Application app) { super(app); mApps = Apps.of(app); }

	private final Apps mApps;

	private static final AtomicInteger sOrderGenerator = new AtomicInteger();
}
