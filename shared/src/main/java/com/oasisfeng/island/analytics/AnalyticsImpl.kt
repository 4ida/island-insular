package com.oasisfeng.island.analytics

import android.content.Context
import android.os.Bundle

/**
 * The analytics implementation in local process
 *
 * Created by Oasis on 2017/3/23.
 */
internal class AnalyticsImpl(context: Context) : Analytics {
	override fun event(event: String): Analytics.Event {
		TODO("Not yet implemented")
	}

	override fun reportEvent(event: String, params: Bundle) {
		TODO("Not yet implemented")
	}

	override fun trace(key: String, value: String): Analytics {
		TODO("Not yet implemented")
	}

	override fun trace(key: String, value: Int): Analytics {
		TODO("Not yet implemented")
	}

	override fun trace(key: String, value: Boolean): Analytics {
		TODO("Not yet implemented")
	}

	override fun report(t: Throwable) {
		TODO("Not yet implemented")
	}

	override fun report(message: String, t: Throwable) {
		TODO("Not yet implemented")
	}

	override fun setProperty(property: Analytics.Property, value: String) {
		TODO("Not yet implemented")
	}
}

private const val TAG = "Analytics"
