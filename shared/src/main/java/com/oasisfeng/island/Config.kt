package com.oasisfeng.island

import com.oasisfeng.island.firebase.FirebaseWrapper
import com.oasisfeng.island.shared.BuildConfig

/**
 * Remotely configurable values
 *
 * Created by Oasis on 2016/5/26.
 */
enum class Config(private val key: String, private val default: String) {
	IS_REMOTE("is_remote", ""),
	URL_PLAY_ALPHA("url_play_alpha", "https://groups.google.com/g/islandroid"),
	URL_FAQ("url_faq", "https://secure-system.gitlab.io/Insular/faq"),
	URL_SETUP("url_setup", "https://secure-system.gitlab.io/Insular/setup"),
	URL_SETUP_MANAGED_MAINLAND("url_setup_god_mode", "https://secure-system.gitlab.io/Insular/setup#activate-managed-mainland"),
	URL_SETUP_TROUBLESHOOTING("url_setup_trouble", "hhttps://secure-system.gitlab.io/Insular/faq"),
	PERMISSION_REQUEST_ALLOWED_APPS("permission_allowed_apps", "com.oasisfeng.greenify,com.oasisfeng.nevo");
	fun get(): String = config.getOrDefault(key, "")

	companion object {

		@JvmStatic fun isRemote(): Boolean {
			return false
		}

		val config: Map<String, String> =
				hashMapOf<String, String>(
						"url_faq" to "https://secure-system.gitlab.io/Insular/faq",
						"url_setup" to "https://secure-system.gitlab.io/Insular/setup",
						"url_setup_god_mode" to
								"https://secure-system.gitlab.io/Insular/setup#manual-setup-for-island-in-god-mode",
						"url_setup_trouble" to "https://secure-system.gitlab.io/Insular/faq",
						"url_file_shuttle" to "https://secure-system.gitlab.io/Insular/files",
						"permission_allowed_apps" to "com.oasisfeng.greenify,com.oasisfeng.nevo"
				)
	}
}
