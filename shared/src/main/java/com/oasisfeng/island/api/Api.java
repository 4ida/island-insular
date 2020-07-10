package com.oasisfeng.island.api;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.RequiresPermission;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

/**
 * The API protocol
 *
 * <p>Common results for APIs:
 * <ul>
 * <li>{@link Activity#RESULT_OK} for success
 * <li>{@link Activity#RESULT_CANCELED} for failure
 * <li>{@link latest#RESULT_UNVERIFIED_IDENTITY} if invocation is not allowed. (permission not granted)
 * </ul>
 *
 * <p>API revisions:
 * <ul>
 * <li>v1.0 released in Island v2.0.
 * <li>v1.1 released in Island v2.5 (250).
 * <li>v2.0 released in Island v2.9 (29000).
 * <li>v2.1 released in Island v3.5.1 (35100).
 * </ul>
 *
 * Created by Oasis on 2017/9/19.
 */
public class Api {

	public interface latest extends v2 {}

	interface v2 extends v1 {
		/* Runtime permissions required for certain APIs */
		String PERMISSION_FREEZE_PACKAGE = "com.oasisfeng.island.permission.FREEZE_PACKAGE";
		String PERMISSION_LAUNCH_PACKAGE = "com.oasisfeng.island.permission.LAUNCH_PACKAGE";
		String PERMISSION_SUSPEND_PACKAGE = "com.oasisfeng.island.permission.SUSPEND_PACKAGE";

		/**
		 * Freeze the app(s) specified by Intent data with "package" or "packages" (comma-separated) scheme.
		 * <p>Result: {@link Activity#RESULT_OK} for success, {@link Activity#RESULT_CANCELED} for failure or {@link latest#RESULT_UNVERIFIED_IDENTITY}
		 */
		@Since(2.0) @RequiresPermission(PERMISSION_FREEZE_PACKAGE) String ACTION_FREEZE = v1.ACTION_FREEZE;

		/**
		 * Unfreeze the app(s) specified by Intent data with "package" or "packages" (comma-separated) scheme.
		 * <p>Result: {@link Activity#RESULT_OK} for success, {@link Activity#RESULT_CANCELED} for failure or {@link latest#RESULT_UNVERIFIED_IDENTITY}
		 */
		@Since(2.0) @RequiresPermission(PERMISSION_FREEZE_PACKAGE) String ACTION_UNFREEZE = v1.ACTION_UNFREEZE;

		/**
		 * Launch the (possibly frozen) app specified by Intent data with "package" scheme. App will be unfrozen before launch.
		 * <p>Result: {@link Activity#RESULT_OK} for success, {@link Activity#RESULT_CANCELED} for failure or {@link latest#RESULT_UNVERIFIED_IDENTITY}
		 */
		@Since(2.0) @RequiresPermission(PERMISSION_LAUNCH_PACKAGE) String ACTION_LAUNCH = v1.ACTION_LAUNCH;

		/**
		 * Suspend the app(s) specified by Intent data with "package" or "packages" (comma-separated) scheme. (requires Android N+)
		 * <p>Result: {@link Activity#RESULT_OK} for success, {@link Activity#RESULT_CANCELED} for failure or {@link latest#RESULT_UNVERIFIED_IDENTITY}
		 */
		@Since(2.1) @RequiresPermission(PERMISSION_SUSPEND_PACKAGE) String ACTION_SUSPEND = "com.oasisfeng.island.action.SUSPEND";

		/**
		 * Unsuspend the app(s) specified by Intent data with "package" or "packages" (comma-separated) scheme. (requires Android N+)
		 * <p>Result: {@link Activity#RESULT_OK} for success, {@link Activity#RESULT_CANCELED} for failure or {@link latest#RESULT_UNVERIFIED_IDENTITY}
		 */
		@Since(2.1) @RequiresPermission(PERMISSION_SUSPEND_PACKAGE) String ACTION_UNSUSPEND = "com.oasisfeng.island.action.UNSUSPEND";

		/**
		 * {@link android.app.PendingIntent} extra required for all types of API invocation except {@link Activity#startActivityForResult(Intent, int)}.
		 * The creator of this PendingIntent will be treated as API caller.
		 *
		 * IMPORTANT: The intent of this PendingIntent MUST NOT have action, categories and data.
		 */
		@Since(2.0) String EXTRA_CALLER_ID = v1.EXTRA_CALLER_ID;
	}

	interface v1 {
		@Since(1.0) String ACTION_FREEZE   = "com.oasisfeng.island.action.FREEZE";
		@Since(1.1) String ACTION_UNFREEZE = "com.oasisfeng.island.action.UNFREEZE";

		/** Data: Activity intent to launch, in "intent:" scheme. {@link Intent#EXTRA_USER} is supported for shuttle. */
		@RestrictTo(LIBRARY)	// Internal API
		@Since(1.1) String ACTION_LAUNCH   = "com.oasisfeng.island.action.LAUNCH";

		/**
		 * {@link android.app.PendingIntent} extra required for all types of API invocation except {@link Activity#startActivityForResult(Intent, int)}
		 * The creator of this PendingIntent will be treated as API caller.
		 */
		@Since(1.0) String EXTRA_CALLER_ID = "caller";

		/** Result code for unverified identity. */
		@Since(1.0) int RESULT_UNVERIFIED_IDENTITY = Activity.RESULT_FIRST_USER;
	}

	@Retention(RetentionPolicy.SOURCE) private @interface Since { @SuppressWarnings("unused") double value(); }
}
