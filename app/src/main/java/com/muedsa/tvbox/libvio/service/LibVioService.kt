package com.muedsa.tvbox.libvio.service

import com.muedsa.tvbox.tool.feignChrome
import com.muedsa.tvbox.tool.get
import com.muedsa.tvbox.tool.parseHtml
import com.muedsa.tvbox.tool.toRequestBuild
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient

class LibVioService(
    private val okHttpClient: OkHttpClient
) {

    private var siteUrl: String? = null
    private val mutex = Mutex()

    suspend fun getSiteUrl(): String = mutex.withLock {
        if (siteUrl == null) {
            val body = "https://libfabu.com".toRequestBuild()
                .feignChrome()
                .get(okHttpClient = okHttpClient)
                .parseHtml()
                .body()
            siteUrl = body.selectFirst("#all .content .content-top ul li")
                ?.select("a[href]")
                ?.map { it.attr("href").removeSuffix("/") }
                ?.firstOrNull()
                ?: throw RuntimeException("从发布页获取可用地址失败")
        }
        return siteUrl!!
    }
}