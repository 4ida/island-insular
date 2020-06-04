package com.oasisfeng.island.analytics;

import android.os.Bundle;
import android.util.Log;

import org.intellij.lang.annotations.Pattern;

import javax.annotation.ParametersAreNonnullByDefault;

import androidx.annotation.CheckResult;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

/**
 * Abstraction for analytics service
 *
 * Created by Oasis on 2016/5/26.
 */
@ParametersAreNonnullByDefault
public interface Analytics {

	interface Event {
		@CheckResult Event withRaw(String key, @Nullable String value);
		void send();
	}

	@CheckResult Event event(@Size(min = 1, max = 40) @Pattern("^[a-zA-Z][a-zA-Z0-9_]*$") String event);
	void reportEvent(String event, Bundle params);
	Analytics trace(String key, String value);
	Analytics trace(String key, int value);
	Analytics trace(String key, boolean value);
	void report(Throwable t);
	void report(String message, Throwable t);
	default void logAndReport(final String tag, final String message, final Throwable t) {
		Log.e(tag, message, t);
		report(message, t);
	}

	interface Trace {
		void start();
		void stop();
		void incrementCounter(String counter_name, long increment_by);
		default void incrementCounter(final String counter_name) { incrementCounter(counter_name, 1); }
	}

	enum Property {
		DeviceOwner("device_owner"),
		IslandSetup("island_setup"),
		EncryptionRequired("encryption_required"),
		DeviceEncrypted("device_encrypted"),
		RemoteConfigAvailable("remote_config_avail"),
		FileShuttleEnabled("file_shuttle_enabled"),
		;
		Property(final String name) { this.name = name; }

		final String name;
	}

	void setProperty(Property property, @Size(max = 36) String value);
	default Analytics setProperty(final Property property, final boolean value) {
		setProperty(property, Boolean.toString(value));
		return this;
	}

	static Analytics $() { return AnalyticsImpl.$(); }
}
