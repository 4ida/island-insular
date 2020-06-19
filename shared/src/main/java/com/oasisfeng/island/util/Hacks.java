package com.oasisfeng.island.util;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.print.PrintManager;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.oasisfeng.hack.Hack;
import com.oasisfeng.hack.Hack.Unchecked;
import com.oasisfeng.island.analytics.Analytics;
import com.oasisfeng.island.shared.BuildConfig;

import java.io.File;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.N;

/**
 * All reflection-based hacks should be defined here
 *
 * Created by Oasis on 2016/8/10.
 */
public class Hacks {

	static {
		Hack.setAssertionFailureHandler(e -> {
			Log.e("Compatibility", e.getDebugInfo());
			if (BuildConfig.DEBUG) throw new IllegalStateException("Incompatibility", e);
			if (Users.isOwner()) Analytics.$().report(e);
		});
	}

	private static final int MATCH_ANY_USER = 0x00400000;		// Requires INTERACT_ACROSS_USERS since Android P.
	/**
	 * When used in @ApplicationInfoFlags or @PackageInfoFlags:
	 *   For owner user, GET_UNINSTALLED_PACKAGES implicitly set MATCH_ANY_USER.
	 *   For managed profile, MATCH_ANY_USER requires permission INTERACT_ACROSS_USERS since Android P.
	 * When used in @ComponentInfoFlags or @ResolveInfoFlags: MATCH_ANY_USER is always allowed.
	 *
	 * See PackageManagerService.updateFlagsForPackage()
	 */
	public static final int GET_ANY_USER_AND_UNINSTALLED = PackageManager.GET_UNINSTALLED_PACKAGES | (Users.isOwner() ? 0 : MATCH_ANY_USER);
	public static final int RESOLVE_ANY_USER_AND_UNINSTALLED = PackageManager.GET_UNINSTALLED_PACKAGES | MATCH_ANY_USER;

	public static final Hack.HackedField<ApplicationInfo, Integer>
			ApplicationInfo_privateFlags = Hack.onlyIf(SDK_INT >= M).into(ApplicationInfo.class).field("privateFlags").fallbackTo(null);
	public static final Hack.HackedField<ApplicationInfo, Integer>
			ApplicationInfo_versionCode = Hack.into(ApplicationInfo.class).field("versionCode").fallbackTo(0);
	public static final Hack.HackedTargetField<String>
			PrintManager_PRINT_SPOOLER_PACKAGE_NAME = Hack.onlyIf(SDK_INT >= N && ! Builds.isAndroidPIncludingPreviews()).into(PrintManager.class)
			.staticField("PRINT_SPOOLER_PACKAGE_NAME").fallbackTo("com.android.printspooler");
	public static final Hack.HackedField<PowerManager, Object>
			PowerManager_mService = Hack.into(PowerManager.class).field("mService").fallbackTo(null);

	public static final Hack.HackedMethod2<Boolean, Void, Unchecked, Unchecked, Unchecked, String, Boolean>
			SystemProperties_getBoolean = Hack.into("android.os.SystemProperties").staticMethod("getBoolean")
			.returning(boolean.class).fallbackReturning(false).withParams(String.class, boolean.class);
	public static final Hack.HackedMethod2<Integer, Void, Unchecked, Unchecked, Unchecked, String, Integer>
			SystemProperties_getInt = Hack.into("android.os.SystemProperties").staticMethod("getInt")
			.returning(int.class).fallbackReturning(null).withParams(String.class, int.class);
	static final Hack.HackedMethod1<ComponentName, DevicePolicyManager, IllegalArgumentException, Unchecked, Unchecked, Integer>
			DevicePolicyManager_getProfileOwnerAsUser = Hack.into(DevicePolicyManager.class).method("getProfileOwnerAsUser")
			.returning(ComponentName.class).fallbackReturning(null).throwing(IllegalArgumentException.class).withParam(int.class);
	static final Hack.HackedMethod0<String, DevicePolicyManager, Unchecked, Unchecked, Unchecked>
			DevicePolicyManager_getDeviceOwner = Hack.into(DevicePolicyManager.class).method("getDeviceOwner")
			.returning(String.class).fallbackReturning(null).withoutParams();
	@RequiresApi(N) public static final @Nullable Hack.HackedMethod3<ApplicationInfo, LauncherApps, Exception, Unchecked, Unchecked, String, Integer, UserHandle>
			LauncherApps_getApplicationInfo = SDK_INT < N ? null : Hack.into(LauncherApps.class).method("getApplicationInfo").throwing()	// NameNotFoundException added since Android O.
			.returning(ApplicationInfo.class).withParams(String.class, int.class, UserHandle.class);
	public static final Hack.HackedMethod4<Boolean, Context, Unchecked, Unchecked, Unchecked, Intent, ServiceConnection, Integer, UserHandle>
			Context_bindServiceAsUser = Hack.into(Context.class).method("bindServiceAsUser").returning(boolean.class)
			.fallbackReturning(false).withParams(Intent.class, ServiceConnection.class, int.class, UserHandle.class);
	@RequiresApi(N) public static final @Nullable Hack.HackedMethod2<int[], UserManager, Unchecked, Unchecked, Unchecked, Integer, Boolean>
			UserManager_getProfileIds = SDK_INT < N || Builds.isAndroidPIncludingPreviews() ? null : Hack.into(UserManager.class).method("getProfileIds")
			.returning(int[].class).withParams(int.class, boolean.class);
	public static final Hack.HackedMethod3<Context, Context, NameNotFoundException, Unchecked, Unchecked, String, Integer, UserHandle>
			Context_createPackageContextAsUser = Hack.into(Context.class).method("createPackageContextAsUser").returning(Context.class)
			.fallbackReturning(null).throwing(NameNotFoundException.class).withParams(String.class, int.class, UserHandle.class);
	public static final @Nullable Hack.HackedMethodN<IBinder, Void, Unchecked, Unchecked, Unchecked>
			ServiceManager_getService = Hack.into("android.os.ServiceManager").staticMethod("getService")
			.returning(IBinder.class).withParams(String.class);
	private static final String IWebViewUpdateService = "android.webkit.IWebViewUpdateService";
	public static final @Nullable Hack.HackedMethod1<?, Void, Unchecked, Unchecked, Unchecked, IBinder>
			IWebViewUpdateService$Stub_asInterface = Hack.into(IWebViewUpdateService + "$Stub").staticMethod("asInterface")
			.returning(Hack.ANY_TYPE).withParam(IBinder.class);
	@RequiresApi(N) public static final @Nullable Hack.HackedMethod0<String, Object, RemoteException, Unchecked, Unchecked>
			IWebViewUpdateService_getCurrentWebViewPackageName = SDK_INT < N ? null :
			Hack.into(IWebViewUpdateService).method("getCurrentWebViewPackageName")
			.returning(String.class).throwing(RemoteException.class).withoutParams();
	public static final @Nullable Hack.HackedMethod0<File, Void, Unchecked, Unchecked, Unchecked>
			Environment_getDataSystemDirectory = Hack.into(Environment.class)
			.staticMethod(SDK_INT < N ? "getSystemSecureDirectory" : "getDataSystemDirectory").returning(File.class).withoutParams();

}
