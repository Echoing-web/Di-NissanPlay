package com.shilapi.xcertplay.bluetooth

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * [AnwServiceProbe] 的 Android 生产实现：以服务 action + 包名双匹配解析
 * AnwPhoneLink 厂商服务（SDD 检测门；对齐 T32 实机路线——包名存在不等于
 * 服务可达，必须 queryIntentServices 命中）。
 */
class AndroidAnwServiceProbe(private val context: Context) : AnwServiceProbe {
    override fun queryServiceCount(action: String, packageName: String): Int = runCatching {
        context.packageManager.queryIntentServices(
            Intent(action).setPackage(packageName),
            PackageManager.GET_META_DATA,
        ).size
    }.getOrDefault(0)
}
