package com.oasisfeng.island.api;

import android.app.AppOpsManager;
import android.app.DerivedAppOpsManager;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.oasisfeng.android.annotation.UserIdInt;
import com.oasisfeng.android.os.UserHandles;
import com.oasisfeng.hack.Hack;
import com.oasisfeng.island.RestrictedBinderProxy;
import com.oasisfeng.island.appops.AppOpsHelper;
import com.oasisfeng.island.shuttle.Shuttle;
import com.oasisfeng.island.util.Hacks;
import com.oasisfeng.island.util.Users;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import kotlin.Unit;

import static android.content.Context.APP_OPS_SERVICE;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.P;
import static com.oasisfeng.island.ApiConstants.DELEGATION_APP_OPS;

/**
 * Delegated {@link AppOpsManager}
 *
 * Created by Oasis on 2019-4-30.
 */
public class DelegatedAppOpsManager extends DerivedAppOpsManager {

	public static Binder buildBinderProxy(final Context context) throws ReflectiveOperationException {
		return new DelegatedAppOpsManager(context).mBinderProxy;
	}

	private DelegatedAppOpsManager(final Context context) throws ReflectiveOperationException {
		mBinderProxy = sHelper.inject(this, context, APP_OPS_SERVICE, (c, delegate) -> new AppOpsBinderProxy(c, DELEGATION_APP_OPS, delegate));
		final Hacks.AppOpsManager aom = Hack.into(this).with(Hacks.AppOpsManager.class);

		// Supported APIs. (not the following but the indirectly invoked APIs on IAppOpsService)
		aom.setMode(0, 0, "a.b.c", 0);  // Must be the first one. See AppOpsBinderProxy.onTransact() below.
		aom.setUidMode(AppOpsManager.OPSTR_CAMERA, 0, 0);
		aom.getOpsForPackage(0, "a.b.c", new int[]{ 0 });
		aom.getPackagesForOps(new int[]{ 0 });
		aom.setRestriction(0, 0, 0, null);
		aom.resetAllModes();

		mBinderProxy.seal();
	}

	private final AppOpsBinderProxy mBinderProxy;

	private static final DerivedManagerHelper<AppOpsManager> sHelper = new DerivedManagerHelper<>(AppOpsManager.class);

	private static class AppOpsBinderProxy extends RestrictedBinderProxy {

		@Override protected boolean onTransact(final int code, @NonNull final Parcel data, @Nullable final Parcel reply, final int flags) throws RemoteException {
			if (SDK_INT >= P && ! isSealed() && mCodeSetMode == - 1) mCodeSetMode = code;
			return super.onTransact(code, data, reply, flags);
		}

		@Override protected boolean doTransact(final int code, final Parcel data, final Parcel reply, final int flags) throws RemoteException {
			if (code == mCodeSetMode && SDK_INT >= P) return setMode(data);
			else return super.doTransact(code, data, reply, flags);
		}

		@RequiresApi(P) protected boolean setMode(final Parcel data) throws RemoteException {
			if (SDK_INT < P) throw new SecurityException("Island has no privilege to setMode() before Android P.");

			data.enforceInterface(DESCRIPTOR);
			final int op = data.readInt();
			final int uid = data.readInt();
			final String pkg = data.readString();
			if (pkg == null) throw new NullPointerException("packageName is null");
			final int mode = data.readInt();
			Log.i(TAG, "IAppOpsService.setMode(" + op + ", " + uid + ", " + pkg + ", " + mode + ")");

			final @UserIdInt int user_id = UserHandles.getUserId(uid);
			if (user_id == UserHandles.getIdentifier(Users.current())) {
				new AppOpsHelper(mContext).setMode(pkg, op, mode, uid);
				return true;
			}
			final UserHandle user = UserHandles.of(user_id);
			if (! Users.isProfileManagedByIsland(user))
				throw new IllegalArgumentException("User " + user_id + " is not managed by Island");

			try {	// Cross-profile synchronized invocation with 2s timeout.
				final CompletableFuture<Unit> future = new Shuttle(mContext, user).launchAsFuture(context -> {
					new AppOpsHelper(context).setMode(pkg, op, mode, uid); return Unit.INSTANCE;
				});
				if (future != null) future.get(2, TimeUnit.SECONDS);
			} catch (final ExecutionException e) {
				final Throwable cause = e.getCause();
				if (cause instanceof RuntimeException) throw (RuntimeException) cause;
				if (cause instanceof RemoteException) throw (RemoteException) cause;
				throw new RemoteException("Failed to setMode() due to " + e.getCause());
			} catch (final InterruptedException | TimeoutException ignored) {}
			return true;
		}

		AppOpsBinderProxy(final Context context, final String delegation, final IBinder delegate) { super(context, delegation, delegate); }

		private int mCodeSetMode = -1;
		private static final java.lang.String DESCRIPTOR = "com.android.internal.app.IAppOpsService";
	}

	private static final String TAG = "Island.DAOM";
}
