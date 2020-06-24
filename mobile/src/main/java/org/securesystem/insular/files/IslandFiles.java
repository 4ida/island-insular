package org.securesystem.insular.files;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.annotation.RequiresPermission;

import org.securesystem.insular.mobile.R;
import org.securesystem.insular.shuttle.ContextShuttle;
import org.securesystem.insular.util.Modules;
import org.securesystem.insular.util.Permissions;
import org.securesystem.insular.util.Users;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
import static android.content.pm.PackageManager.DONT_KILL_APP;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.M;

/**
 * Created by Oasis on 2018-6-8.
 */
public class IslandFiles {

	public static boolean isCompatible(final Context context) {
		return Users.hasProfile() && getProfilePackageManager(context) != null;
	}

	@RequiresPermission(Permissions.INTERACT_ACROSS_USERS) public static boolean isFileShuttleEnabled(final Context context) {
		final ComponentName component = Modules.getFileProviderComponent(context);
		return component != null && getProfilePackageManager(context).getComponentEnabledSetting(component) == COMPONENT_ENABLED_STATE_ENABLED;
	}

	@RequiresPermission(Permissions.INTERACT_ACROSS_USERS) public static void enableFileShuttle(final Activity activity) {
		if (Modules.getFileProviderComponent(activity) == null) {
			Toast.makeText(activity, "Module \"File Provider\" not installed.", Toast.LENGTH_LONG).show();
			return;
		}
		if (SDK_INT >= M) {
			com.oasisfeng.android.content.pm.Permissions.request(activity, WRITE_EXTERNAL_STORAGE, result -> {
				if (result == PERMISSION_GRANTED) onPermissionGranted(activity);
				else Toast.makeText(activity, R.string.toast_external_storage_permission_required, Toast.LENGTH_LONG).show();
			});

		} else onPermissionGranted(activity);
	}

	private static void onPermissionGranted(final Context context) {
		
		final ComponentName component = Modules.getFileProviderComponent(context);
		getProfilePackageManager(context).setComponentEnabledSetting(component, COMPONENT_ENABLED_STATE_ENABLED, DONT_KILL_APP);
		// FIXME: Not enabling shuttle in owner user, as ExternalStorageProviderProxy is not working as expected in manger profile.
		//setComponentEnabled(context.getPackageManager(), shuttle, all_met);
	}

	private static PackageManager getProfilePackageManager(final Context context) {
		return ContextShuttle.getPackageManagerAsUser(context, Users.profile);
	}
}
