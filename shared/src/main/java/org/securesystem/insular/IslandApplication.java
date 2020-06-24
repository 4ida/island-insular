package org.securesystem.insular;

import android.app.Application;

import org.securesystem.insular.firebase.FirebaseServiceProxy;
import org.securesystem.insular.shared.R;

/**
 * For singleton instance purpose only.
 *
 * Created by Oasis on 2018/1/3.
 */
public class IslandApplication extends Application {

	public static Application $() {
		return sInstance;
	}

	public IslandApplication() {
		if (sInstance != null) throw new IllegalStateException("Already initialized");
		sInstance = this;
	}

	@Override public void onCreate() {
		super.onCreate();
		final String firebase_proxy_host = getString(R.string.firebase_proxy_host);
		if (! firebase_proxy_host.isEmpty()) FirebaseServiceProxy.initialize(firebase_proxy_host);
	}

	private static IslandApplication sInstance;
}
