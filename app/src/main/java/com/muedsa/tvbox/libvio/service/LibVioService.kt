package com.muedsa.tvbox.libvio.service

import com.muedsa.tvbox.tool.checkSuccess
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
            siteUrl = checkUrl(getSiteUrls())
            if (siteUrl.isNullOrBlank()) {
                siteUrl = checkUrl(URLS)
            }
            if (siteUrl.isNullOrBlank()) {
                throw RuntimeException("从发布页获取可用地址失败")
            }
        }
        return siteUrl!!
    }

    fun checkUrl(urls: List<String>): String {
        var url = ""
        for (urlFromFromReleasePage in urls) {
            try {
                url = checkUrl(urlFromFromReleasePage)
                break
            } catch (_: Throwable) {}
        }
        return url
    }

    fun checkUrl(url: String): String {
        if (url.isBlank()) throw RuntimeException("url is empty")
        url.toRequestBuild()
            .feignChrome()
            .get(okHttpClient = okHttpClient)
            .checkSuccess()
        return url
    }

    private fun getSiteUrls(): List<String> {
        for (releasePageUrl in RELEASE_PAGE_URLS) {
            val siteUrls = getSiteUrls(releasePageUrl)
            if (siteUrls.isNotEmpty()) {
                return siteUrls
            }
        }
        return emptyList()
    }

    private fun getSiteUrls(url: String): List<String> {
        return try {
            val body = url.toRequestBuild()
                .feignChrome()
                .get(okHttpClient = okHttpClient)
                .checkSuccess()
                .parseHtml()
                .body()
            body.selectFirst("#all .content .content-top ul li")
                ?.select(">a[href]")
                ?.map { it.attr("href").removeSuffix("/") }
                ?.filter { it.startsWith("https://") }
                ?: emptyList()
        } catch (_: Throwable) { emptyList() }
    }

    companion object {
        val RELEASE_PAGE_URLS = listOf(
            "https://lib.ifabu.vip",
            "https://libvio.app",
            "https://libfabu.com",
        )

        val URLS = listOf(
            "https://libvio.cc",
            "https://libvio.cloud",
            "https://libvio.mov",
            "https://libvio.vip",
        )
    }
}