package com.muedsa.tvbox.libvio

import com.muedsa.tvbox.libvio.service.LibVioService
import com.muedsa.tvbox.libvio.service.MediaDetailService
import com.muedsa.tvbox.tool.LenientJson
import com.muedsa.tvbox.tool.createOkHttpClient
import com.muedsa.tvbox.tool.feignChrome
import com.muedsa.tvbox.tool.get
import com.muedsa.tvbox.tool.stringBody
import com.muedsa.tvbox.tool.toRequestBuild
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.intArrayOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class UrlContentValidator {

    private val okHttpClient = createOkHttpClient(debug = true)

    private val libVioService = LibVioService(
        okHttpClient = okHttpClient,
    )

    @Test
    fun playerConfigJs_valid() = runTest{
        val baseUrl = libVioService.getSiteUrl()
        val content = "${baseUrl}/static/js/playerconfig.js".toRequestBuild()
            .feignChrome(referer = baseUrl)
            .get(okHttpClient = okHttpClient)
            .stringBody()
        val json = PLAYER_LIST_REGEX.find(content)?.groups[1]?.value
            ?: throw RuntimeException("PLAYER_LIST_REGEX fail")
        val configMap = LenientJson.decodeFromString<Map<String, Map<String, String>>>(json)
            .filter { it.key.isNotBlank() && IGNORE_PLAYERS.all { ig -> !it.key.startsWith(ig) }}
        for ((player, config) in configMap) {
            val playerUrl = MediaDetailService.PLAYER_URL_MAP[player]
            checkNotNull(playerUrl) { "新的播放源 $player: $config" }
            if (playerUrl.isNotBlank()) {
                playerJsValid(player, playerUrl)
            }
        }
    }

    private fun playerJsValid(player: String, playerUrl: String) = runTest {
        val baseUrl = libVioService.getSiteUrl()
        val content = "${baseUrl}/static/player/${player}.js".toRequestBuild()
            .feignChrome(referer = baseUrl)
            .get(okHttpClient = okHttpClient)
            .stringBody()
        val path = playerUrl.replaceAfter("?", "")
            .removeSuffix("?")
            .removeSuffix("/")
        checkNotNull(content.contains(path)) { "播放源更新 $player:\n$content" }
    }

    companion object {
        val PLAYER_LIST_REGEX = "MacPlayerConfig\\.player_list=(\\{.*?\\}),MacPlayerConfig".toRegex()
        val IGNORE_PLAYERS = listOf("iframe", "banquan", "iframe", "dplayer", "uc", "kuake",
            "xunlei", "yc", "LINE1080")
    }
}