package com.oasisfeng.island;

import java.util.HashMap;
import java.util.Map;

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
		return config.get(key);
	}

	public static boolean isRemote() {
		return false;
	}

	Config(final String key) { this.key = key; }

	private final String key;

	private static Map<String, String> config = new HashMap<String, String>() {{
		put("url_faq", "https://island.oasisfeng.com/faq");
		put("url_setup", "https://island.oasisfeng.com/setup");
		put("url_setup_god_mode", "https://island.oasisfeng.com/setup#manual-setup-for-island-in-god-mode");
		put("url_setup_trouble", "https://island.oasisfeng.com/faq");
		put("url_file_shuttle", "https://island.oasisfeng.com/files");
		put("permission_allowed_apps", "com.oasisfeng.greenify,com.oasisfeng.nevo");
	}};


}
