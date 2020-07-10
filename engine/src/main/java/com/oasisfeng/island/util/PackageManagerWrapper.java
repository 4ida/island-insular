/*
 * Copyright (C) 2017 Oasis Feng. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oasisfeng.island.util;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ChangedPackages;
import android.content.pm.FeatureInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.SharedLibraryInfo;
import android.content.pm.VersionedPackage;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.annotation.RestrictTo;

import java.util.List;

import static android.os.Build.VERSION_CODES.HONEYCOMB;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.KITKAT_WATCH;
import static android.os.Build.VERSION_CODES.O;

/**
 * Delegation wrapper of {@link PackageManager}
 *
 * Created by Oasis on 2017/3/27.
 */
@Keep @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PackageManagerWrapper extends PackageManager {

	protected PackageManagerWrapper(PackageManager base) { mBase = base; }

	@Override public PackageInfo getPackageInfo(String packageName, int flags) throws NameNotFoundException {
		return mBase.getPackageInfo(packageName, flags);
	}

	@RequiresApi(O) @Override public PackageInfo getPackageInfo(VersionedPackage versionedPackage, int flags) throws NameNotFoundException {
		return mBase.getPackageInfo(versionedPackage, flags);
	}

	@Override public String[] currentToCanonicalPackageNames(String[] names) {
		return mBase.currentToCanonicalPackageNames(names);
	}

	@Override public String[] canonicalToCurrentPackageNames(String[] names) {
		return mBase.canonicalToCurrentPackageNames(names);
	}

	@Override public Intent getLaunchIntentForPackage(String packageName) {
		return mBase.getLaunchIntentForPackage(packageName);
	}

	@Override public Intent getLeanbackLaunchIntentForPackage(String packageName) {
		return mBase.getLeanbackLaunchIntentForPackage(packageName);
	}

	@Override public int[] getPackageGids(String packageName) throws NameNotFoundException {
		return mBase.getPackageGids(packageName);
	}

	@Override public int[] getPackageGids(String packageName, int flags) throws NameNotFoundException {
		return mBase.getPackageGids(packageName, flags);
	}

	@Override public int getPackageUid(String packageName, int flags) throws NameNotFoundException {
		return mBase.getPackageUid(packageName, flags);
	}

	@Override public PermissionInfo getPermissionInfo(String name, int flags) throws NameNotFoundException {
		return mBase.getPermissionInfo(name, flags);
	}

	@Override public List<PermissionInfo> queryPermissionsByGroup(String group, int flags) throws NameNotFoundException {
		return mBase.queryPermissionsByGroup(group, flags);
	}

	@Override public PermissionGroupInfo getPermissionGroupInfo(String name, int flags) throws NameNotFoundException {
		return mBase.getPermissionGroupInfo(name, flags);
	}

	@Override public List<PermissionGroupInfo> getAllPermissionGroups(int flags) {
		return mBase.getAllPermissionGroups(flags);
	}

	@Override public ApplicationInfo getApplicationInfo(String packageName, int flags) throws NameNotFoundException {
		return mBase.getApplicationInfo(packageName, flags);
	}

	@Override public ActivityInfo getActivityInfo(ComponentName component, int flags) throws NameNotFoundException {
		return mBase.getActivityInfo(component, flags);
	}

	@Override public ActivityInfo getReceiverInfo(ComponentName component, int flags) throws NameNotFoundException {
		return mBase.getReceiverInfo(component, flags);
	}

	@Override public ServiceInfo getServiceInfo(ComponentName component, int flags) throws NameNotFoundException {
		return mBase.getServiceInfo(component, flags);
	}

	@Override public ProviderInfo getProviderInfo(ComponentName component, int flags) throws NameNotFoundException {
		return mBase.getProviderInfo(component, flags);
	}

	@Override public List<PackageInfo> getInstalledPackages(int flags) {
		return mBase.getInstalledPackages(flags);
	}

	@RequiresApi(JELLY_BEAN_MR2) @Override public List<PackageInfo> getPackagesHoldingPermissions(String[] permissions, int flags) {
		return mBase.getPackagesHoldingPermissions(permissions, flags);
	}

	@Override public int checkPermission(String permName, String pkgName) {
		return mBase.checkPermission(permName, pkgName);
	}

	@Override public boolean isPermissionRevokedByPolicy(@NonNull String permName, @NonNull String pkgName) {
		return mBase.isPermissionRevokedByPolicy(permName, pkgName);
	}

	@Override public boolean addPermission(PermissionInfo info) {
		return mBase.addPermission(info);
	}

	@Override public boolean addPermissionAsync(PermissionInfo info) {
		return mBase.addPermissionAsync(info);
	}

	@Override public void removePermission(String name) {
		mBase.removePermission(name);
	}

	@Override public int checkSignatures(String pkg1, String pkg2) {
		return mBase.checkSignatures(pkg1, pkg2);
	}

	@Override public int checkSignatures(int uid1, int uid2) {
		return mBase.checkSignatures(uid1, uid2);
	}

	@Override @Nullable public String[] getPackagesForUid(int uid) {
		return mBase.getPackagesForUid(uid);
	}

	@Override @Nullable public String getNameForUid(int uid) {
		return mBase.getNameForUid(uid);
	}

	@Override public List<ApplicationInfo> getInstalledApplications(int flags) {
		return mBase.getInstalledApplications(flags);
	}

	@RequiresApi(O) @Override public boolean isInstantApp() {
		return mBase.isInstantApp();
	}

	@RequiresApi(O) @Override public boolean isInstantApp(String packageName) {
		return mBase.isInstantApp(packageName);
	}

	@RequiresApi(O) @Override public int getInstantAppCookieMaxBytes() {
		return mBase.getInstantAppCookieMaxBytes();
	}

	@RequiresApi(O) @Override public byte[] getInstantAppCookie() {
		return mBase.getInstantAppCookie();
	}

	@RequiresApi(O) @Override public void clearInstantAppCookie() {
		mBase.clearInstantAppCookie();
	}

	@RequiresApi(O) @Override public void updateInstantAppCookie(byte[] cookie) {
		mBase.updateInstantAppCookie(cookie);
	}

	@Override public String[] getSystemSharedLibraryNames() {
		return mBase.getSystemSharedLibraryNames();
	}

	@RequiresApi(O) @Override public List<SharedLibraryInfo> getSharedLibraries(int flags) {
		return mBase.getSharedLibraries(flags);
	}

	@RequiresApi(O) @Override public ChangedPackages getChangedPackages(int sequenceNumber) {
		return mBase.getChangedPackages(sequenceNumber);
	}

	@Override public FeatureInfo[] getSystemAvailableFeatures() {
		return mBase.getSystemAvailableFeatures();
	}

	@Override public boolean hasSystemFeature(String name) {
		return mBase.hasSystemFeature(name);
	}

	@Override public boolean hasSystemFeature(String name, int version) {
		return mBase.hasSystemFeature(name, version);
	}

	@Override public ResolveInfo resolveActivity(Intent intent, int flags) {
		return mBase.resolveActivity(intent, flags);
	}

	@Override public List<ResolveInfo> queryIntentActivities(Intent intent, int flags) {
		return mBase.queryIntentActivities(intent, flags);
	}

	@Override public List<ResolveInfo> queryIntentActivityOptions(ComponentName caller, Intent[] specifics, Intent intent, int flags) {
		return mBase.queryIntentActivityOptions(caller, specifics, intent, flags);
	}

	@Override public List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags) {
		return mBase.queryBroadcastReceivers(intent, flags);
	}

	@Override public ResolveInfo resolveService(Intent intent, int flags) {
		return mBase.resolveService(intent, flags);
	}

	@Override public List<ResolveInfo> queryIntentServices(Intent intent, int flags) {
		return mBase.queryIntentServices(intent, flags);
	}

	@RequiresApi(KITKAT) @Override public List<ResolveInfo> queryIntentContentProviders(Intent intent, int flags) {
		return mBase.queryIntentContentProviders(intent, flags);
	}

	@Override public ProviderInfo resolveContentProvider(String name, int flags) {
		return mBase.resolveContentProvider(name, flags);
	}

	@Override public List<ProviderInfo> queryContentProviders(String processName, int uid, int flags) {
		return mBase.queryContentProviders(processName, uid, flags);
	}

	@Override public InstrumentationInfo getInstrumentationInfo(ComponentName className, int flags) throws NameNotFoundException {
		return mBase.getInstrumentationInfo(className, flags);
	}

	@Override public List<InstrumentationInfo> queryInstrumentation(String targetPackage, int flags) {
		return mBase.queryInstrumentation(targetPackage, flags);
	}

	@Override public Drawable getDrawable(String packageName, int resid, ApplicationInfo appInfo) {
		return mBase.getDrawable(packageName, resid, appInfo);
	}

	@Override public Drawable getActivityIcon(ComponentName activityName) throws NameNotFoundException {
		return mBase.getActivityIcon(activityName);
	}

	@Override public Drawable getActivityIcon(Intent intent) throws NameNotFoundException {
		return mBase.getActivityIcon(intent);
	}

	@RequiresApi(KITKAT_WATCH) @Override public Drawable getActivityBanner(ComponentName activityName) throws NameNotFoundException {
		return mBase.getActivityBanner(activityName);
	}

	@RequiresApi(KITKAT_WATCH) @Override public Drawable getActivityBanner(Intent intent) throws NameNotFoundException {
		return mBase.getActivityBanner(intent);
	}

	@Override public Drawable getDefaultActivityIcon() {
		return mBase.getDefaultActivityIcon();
	}

	@Override public Drawable getApplicationIcon(ApplicationInfo info) {
		return mBase.getApplicationIcon(info);
	}

	@Override public Drawable getApplicationIcon(String packageName) throws NameNotFoundException {
		return mBase.getApplicationIcon(packageName);
	}

	@RequiresApi(KITKAT_WATCH) @Override public Drawable getApplicationBanner(ApplicationInfo info) {
		return mBase.getApplicationBanner(info);
	}

	@RequiresApi(KITKAT_WATCH) @Override public Drawable getApplicationBanner(String packageName) throws NameNotFoundException {
		return mBase.getApplicationBanner(packageName);
	}

	@Override public Drawable getActivityLogo(ComponentName activityName) throws NameNotFoundException {
		return mBase.getActivityLogo(activityName);
	}

	@Override public Drawable getActivityLogo(Intent intent) throws NameNotFoundException {
		return mBase.getActivityLogo(intent);
	}

	@Override public Drawable getApplicationLogo(ApplicationInfo info) {
		return mBase.getApplicationLogo(info);
	}

	@Override public Drawable getApplicationLogo(String packageName) throws NameNotFoundException {
		return mBase.getApplicationLogo(packageName);
	}

	@Override public Drawable getUserBadgedIcon(Drawable icon, UserHandle user) {
		return mBase.getUserBadgedIcon(icon, user);
	}

	@Override public Drawable getUserBadgedDrawableForDensity(Drawable drawable, UserHandle user, Rect badgeLocation, int badgeDensity) {
		return mBase.getUserBadgedDrawableForDensity(drawable, user, badgeLocation, badgeDensity);
	}

	@Override public CharSequence getUserBadgedLabel(CharSequence label, UserHandle user) {
		return mBase.getUserBadgedLabel(label, user);
	}

	@Override public CharSequence getText(String packageName, int resid, ApplicationInfo appInfo) {
		return mBase.getText(packageName, resid, appInfo);
	}

	@Override public XmlResourceParser getXml(String packageName, int resid, ApplicationInfo appInfo) {
		return mBase.getXml(packageName, resid, appInfo);
	}

	@Override public CharSequence getApplicationLabel(ApplicationInfo info) {
		return mBase.getApplicationLabel(info);
	}

	@Override public Resources getResourcesForActivity(ComponentName activityName) throws NameNotFoundException {
		return mBase.getResourcesForActivity(activityName);
	}

	@Override public Resources getResourcesForApplication(ApplicationInfo app) throws NameNotFoundException {
		return mBase.getResourcesForApplication(app);
	}

	@Override public Resources getResourcesForApplication(String appPackageName) throws NameNotFoundException {
		return mBase.getResourcesForApplication(appPackageName);
	}

	@Override public PackageInfo getPackageArchiveInfo(String archiveFilePath, int flags) {
		return mBase.getPackageArchiveInfo(archiveFilePath, flags);
	}

	@RequiresApi(ICE_CREAM_SANDWICH) @Override public void verifyPendingInstall(int id, int verificationCode) {
		mBase.verifyPendingInstall(id, verificationCode);
	}

	@RequiresApi(JELLY_BEAN_MR1) @Override public void extendVerificationTimeout(int id, int verificationCodeAtTimeout, long millisecondsToDelay) {
		mBase.extendVerificationTimeout(id, verificationCodeAtTimeout, millisecondsToDelay);
	}

	@RequiresApi(HONEYCOMB) @Override public void setInstallerPackageName(String targetPackage, String installerPackageName) {
		mBase.setInstallerPackageName(targetPackage, installerPackageName);
	}

	@RequiresPermission(Manifest.permission.DELETE_PACKAGES)
	@Override public String getInstallerPackageName(String packageName) {
		return mBase.getInstallerPackageName(packageName);
	}

	@Override public void addPackageToPreferred(String packageName) {
		mBase.addPackageToPreferred(packageName);
	}

	@Override public void removePackageFromPreferred(String packageName) {
		mBase.removePackageFromPreferred(packageName);
	}

	@Override public List<PackageInfo> getPreferredPackages(int flags) {
		return mBase.getPreferredPackages(flags);
	}

	@Override public void addPreferredActivity(IntentFilter filter, int match, ComponentName[] set, ComponentName activity) {
		mBase.addPreferredActivity(filter, match, set, activity);
	}

	@Override public void clearPackagePreferredActivities(String packageName) {
		mBase.clearPackagePreferredActivities(packageName);
	}

	@Override public int getPreferredActivities(@NonNull List<IntentFilter> outFilters, @NonNull List<ComponentName> outActivities, String packageName) {
		return mBase.getPreferredActivities(outFilters, outActivities, packageName);
	}

	@Override public void setComponentEnabledSetting(ComponentName componentName, int newState, int flags) {
		mBase.setComponentEnabledSetting(componentName, newState, flags);
	}

	@Override public int getComponentEnabledSetting(ComponentName componentName) {
		return mBase.getComponentEnabledSetting(componentName);
	}

	@Override public void setApplicationEnabledSetting(String packageName, int newState, int flags) {
		mBase.setApplicationEnabledSetting(packageName, newState, flags);
	}

	@Override public int getApplicationEnabledSetting(String packageName) {
		return mBase.getApplicationEnabledSetting(packageName);
	}

	@Override public boolean isSafeMode() {
		return mBase.isSafeMode();
	}

	@RequiresApi(O) @Override public void setApplicationCategoryHint(String packageName, int categoryHint) {
		mBase.setApplicationCategoryHint(packageName, categoryHint);
	}

	@Override @NonNull public PackageInstaller getPackageInstaller() {
		return mBase.getPackageInstaller();
	}

	@RequiresApi(O) @Override public boolean canRequestPackageInstalls() {
		return mBase.canRequestPackageInstalls();
	}

	protected PackageManager mBase;
}
