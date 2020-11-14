package com.oasisfeng.island;

import com.oasisfeng.island.firebase.FirebaseWrapper;
import com.oasisfeng.island.shared.BuildConfig;
import com.oasisfeng.island.shared.R;

/**
 * Remotely configurable values, (default values are defined in config_defaults.xml)
 *
 * Created by Oasis on 2016/5/26.
 */
public enum Config {

	/* All keys must be consistent with config_defaults.xml */
	IS_REMOTE("is_remote"),
	URL_FAQ("url_faq"),
	URL_SETUP("url_setup"),
	URL_SETUP_GOD_MODE("url_setup_god_mode"),
	URL_SETUP_TROUBLESHOOTING("url_setup_trouble"),
	URL_FILE_SHUTTLE("url_file_shuttle"),
	PERMISSION_REQUEST_ALLOWED_APPS("permission_allowed_apps");

	public String get() {
		return "";
	}

	public static boolean isRemote() {
		return false;
	}

	Config(final String key) {
	}

}
