package com.oasisfeng.island.analytics;

import android.os.Process;

import androidx.annotation.NonNull;

import com.oasisfeng.android.util.Suppliers;

import java.util.function.Supplier;


/**
 * Lazy initializer for crash handler.
 *
 * Created by Oasis on 2017/7/14.
 */
public abstract class CrashReport {

	static void logException(final Throwable t) { }
	static void log(final String message) {}
	static void setProperty(final String key, final String value) {  }
	static void setProperty(final String key, final int value) { }
	static void setProperty(final String key, final boolean value) { }

	public static void initCrashHandler() {
		final Thread.UncaughtExceptionHandler current_exception_handler = Thread.getDefaultUncaughtExceptionHandler();
		if (! (current_exception_handler instanceof LazyThreadExceptionHandler))
			Thread.setDefaultUncaughtExceptionHandler(new LazyThreadExceptionHandler(current_exception_handler));
	}

	private static class LazyThreadExceptionHandler implements Thread.UncaughtExceptionHandler {

		@Override public void uncaughtException(final @NonNull Thread thread, final @NonNull Throwable e) {
			if (mHandlingUncaughtException) {		// Avoid infinite recursion
				mOriginalHandler.uncaughtException(thread, e);
				return;
			}
			mHandlingUncaughtException = true;

			final Thread.UncaughtExceptionHandler handler = thread.getUncaughtExceptionHandler();
			if (handler != null) handler.uncaughtException(thread, e);	// May re-enter this method if delegate is initialized above.
			mHandlingUncaughtException = false;
		}

		LazyThreadExceptionHandler(final Thread.UncaughtExceptionHandler default_handler) {
			mOriginalHandler = default_handler;
		}

		private final Thread.UncaughtExceptionHandler mOriginalHandler;
		private boolean mHandlingUncaughtException;
	}
}
