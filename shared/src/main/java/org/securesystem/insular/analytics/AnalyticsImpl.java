package org.securesystem.insular.analytics;

import android.content.Context;
import android.os.Bundle;

import org.securesystem.insular.IslandApplication;
import org.securesystem.insular.shared.BuildConfig;

import org.intellij.lang.annotations.Pattern;

import javax.annotation.ParametersAreNonnullByDefault;

import androidx.annotation.CheckResult;
import androidx.annotation.Nullable;

/**
 * The analytics implementation in local process
 *
 * Created by Oasis on 2017/3/23.
 */
@ParametersAreNonnullByDefault
class AnalyticsImpl implements Analytics {

	@Override public @CheckResult Event event(final @Pattern("^[a-zA-Z][a-zA-Z0-9_]*$") String event) {
		final Bundle bundle = new Bundle();
		return new Event() {
			@Override public @CheckResult Event withRaw(final String key, final @Nullable String value) {
				if (value != null) bundle.putString(key, value);
				return this;
			}
			@Override public void send() { reportEvent(event, bundle); }
		};
	}

	@Override public Analytics trace(final String key, final String value) {
		return this;
	}

	@Override public Analytics trace(final String key, final int value) {
		return this;
	}

	@Override public Analytics trace(final String key, final boolean value) {
		return this;
	}

	@Override public void report(final Throwable t) {
	}

	@Override public void report(final String message, final Throwable t) {
	}

	@Override public void reportEvent(final String event, final Bundle params) {
	}

	@Override public void setProperty(final Property property, final String value) {
	}

	private AnalyticsImpl(final Context context) {

		// TODO: De-dup the user identity between Mainland and Island.
	}

	static Analytics $() {
		return sSingleton;
	}

	private static final AnalyticsImpl sSingleton = new AnalyticsImpl(IslandApplication.$());

	private static final String TAG = "Analytics";
}
