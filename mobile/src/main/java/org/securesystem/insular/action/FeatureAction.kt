package org.securesystem.insular.action

import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.AsyncTask
import android.os.Bundle
import android.widget.Toast
import com.oasisfeng.android.widget.Toasts
import org.securesystem.insular.mobile.BuildConfig
import org.securesystem.insular.shortcut.AbstractAppLaunchShortcut
import org.securesystem.insular.shuttle.MethodShuttle
import org.securesystem.insular.util.CallerAwareActivity
import org.securesystem.insular.util.Users

/**
 * Activity to handle app action "Open Feature"
 *
 * Created by Oasis on 2019-7-1.
 */
private const val URI_HOST = "feature"
private const val PACKAGE_GOOGLE_SEARCH = "com.google.android.googlequicksearchbox"
private const val PACKAGE_GOOGLE_ASSISTANT_GO = "com.google.android.apps.assistant"

class FeatureActionActivity : CallerAwareActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent()
        finish()
    }

    private fun handleIntent() {
        val data = intent.data; val caller = callingPackage
        if (BuildConfig.DEBUG) Toast.makeText(this, "$data\nfrom: $caller", Toast.LENGTH_LONG).show()
        if (intent.action != Intent.ACTION_VIEW || data?.host != URI_HOST || data.pathSegments.size < 1) return

        val query = data.pathSegments[0]
        AsyncTask.execute {
            findApp(query)?.also { activity ->
                val pkg = activity.componentName.packageName
                MethodShuttle.runInProfile(this) { context -> AbstractAppLaunchShortcut.launchApp(context, pkg) }
            } ?: Toasts.showLong(this, "Not found: $query")
        }
    }

    private fun findApp(query: String): LauncherActivityInfo? {     // TODO: Support frozen apps in Island
        (getSystemService(LAUNCHER_APPS_SERVICE) as LauncherApps).getActivityList(null, Users.profile).also { candidates ->
            if (query.all(Char::isLetterOrDigit))
                candidates.filter { candidate -> candidate.componentName.packageName.contains(query, ignoreCase = true) }.apply {
                    if (size in 1..3) return this[0]        // Not a good query word if more than 3 matches
                }   // TODO: More sophisticated matching
            candidates.firstOrNull { candidate -> candidate.label.contains(query, ignoreCase = true) }?.apply { return this }
        }
        return null
    }
}