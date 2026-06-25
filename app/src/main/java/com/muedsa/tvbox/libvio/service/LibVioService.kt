package com.muedsa.tvbox.libvio.service

import com.muedsa.tvbox.tool.checkSuccess
import com.muedsa.tvbox.tool.decodeBase64ToStr
import com.muedsa.tvbox.tool.feignChrome
import com.muedsa.tvbox.tool.get
import com.muedsa.tvbox.tool.parseHtml
import com.muedsa.tvbox.tool.stringBody
import com.muedsa.tvbox.tool.toRequestBuild
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import timber.log.Timber

class LibVioService(
    private val okHttpClient: OkHttpClient
) {

    private var siteUrl: String? = null
    private val mutex = Mutex()

    suspend fun getSiteUrl(): String = mutex.withLock {
        if (siteUrl == null) {
            siteUrl = checkUrl(getUrlsFromGithubReop())
            if (siteUrl.isNullOrBlank()) {
                Timber.w("从Github仓库获取URL失败，尝试本地URL")
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

    fun getUrlsFromGithubReop(): List<String> {
        var content = ""
        for (url in GITHUB_REPO_FILE_URLS) {
            try {
                content = url.toRequestBuild()
                    .feignChrome()
                    .get(okHttpClient = okHttpClient)
                    .checkSuccess()
                    .stringBody()
                break
            } catch (_: Throwable) {}
        }
        return if (content.isBlank()) {
            emptyList()
        } else {
            content.split("\n").map { it.decodeBase64ToStr() }
        }
    }

    companion object {
        val GITHUB_REPO_FILE_URLS = listOf(
            "https://ghfast.top/https://raw.githubusercontent.com/muedsa/libvio-plugin/refs/heads/main/urls",
            "https://gh-proxy.com/raw.githubusercontent.com/muedsa/libvio-plugin/refs/heads/main/urls",
            "https://raw.githubusercontent.com/muedsa/libvio-plugin/refs/heads/main/urls",
        )
        val URLS = listOf(
            "https://libvio.host",
            "https://www.libvio.pw",
            "https://www.libvios.com",
        )
    }
}