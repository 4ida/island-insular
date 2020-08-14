package com.oasisfeng.island.data.helper

import android.content.pm.ApplicationInfo
import com.oasisfeng.island.util.Hacks

inline val ApplicationInfo.installed: Boolean
	get() = flags and ApplicationInfo.FLAG_INSTALLED != 0

inline val ApplicationInfo.isSystem: Boolean
	get() = flags and ApplicationInfo.FLAG_SYSTEM != 0

inline val ApplicationInfo.suspended: Boolean
	get() = flags and ApplicationInfo.FLAG_SUSPENDED != 0

inline val ApplicationInfo.hidden: Boolean
	get() = Hacks.ApplicationInfo_privateFlags[this]?.and(PRIVATE_FLAG_HIDDEN) == PRIVATE_FLAG_HIDDEN

const val PRIVATE_FLAG_HIDDEN = 1
