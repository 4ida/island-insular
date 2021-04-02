package com.oasisfeng.island.shuttle;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.UserHandle;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

import com.oasisfeng.android.content.pm.Permissions;
import com.oasisfeng.island.util.Hacks;

/**
 * Utility class for cross-user context related stuffs.
 *
 * Created by Oasis on 2017/9/1.
 */
public class ContextShuttle {

	@RequiresPermission(Permissions.INTERACT_ACROSS_USERS)
	public static @Nullable PackageManager getPackageManagerAsUser(final Context context, final UserHandle user) {
		try {
			final Context user_context = createContextAsUser(context, user);
			return user_context != null ? user_context.getPackageManager() : null;
		} catch (final NameNotFoundException ignored) { return null; }		// Should never happen
	}

	@RequiresPermission(Permissions.INTERACT_ACROSS_USERS)
	public static @Nullable Context createPackageContextAsUser(final Context context, final String pkg, final UserHandle user) throws NameNotFoundException {
		return Hacks.Context_createPackageContextAsUser.invoke(pkg, 0, user).on(context);
	}

	public static @Nullable Context createContextAsUser(final Context context, final UserHandle user) throws NameNotFoundException {
		return createPackageContextAsUser(context, "system", user);
	}
}
