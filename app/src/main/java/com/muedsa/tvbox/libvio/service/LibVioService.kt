package com.muedsa.tvbox.libvio.service

import com.muedsa.tvbox.libvio.feignChrome
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jsoup.Jsoup

class LibVioService {

    private var siteUrl: String? = null
    private val mutex = Mutex()

    suspend fun getSiteUrl(): String = mutex.withLock {
        if (siteUrl == null) {
            val body = Jsoup.connect("https://libfabu.com")
                .feignChrome()
                .get()
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