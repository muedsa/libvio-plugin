package com.muedsa.tvbox.libvio.service

import com.muedsa.tvbox.api.data.DanmakuData
import com.muedsa.tvbox.api.data.DanmakuDataFlow
import com.muedsa.tvbox.api.data.MediaCard
import com.muedsa.tvbox.api.data.MediaCardRow
import com.muedsa.tvbox.api.data.MediaDetail
import com.muedsa.tvbox.api.data.MediaEpisode
import com.muedsa.tvbox.api.data.MediaHttpSource
import com.muedsa.tvbox.api.data.MediaPlaySource
import com.muedsa.tvbox.api.data.SavedMediaCard
import com.muedsa.tvbox.api.service.IMediaDetailService
import com.muedsa.tvbox.libvio.LibVidConst
import com.muedsa.tvbox.libvio.model.PlayerAAAA
import com.muedsa.tvbox.tool.LenientJson
import com.muedsa.tvbox.tool.checkSuccess
import com.muedsa.tvbox.tool.decodeBase64ToStr
import com.muedsa.tvbox.tool.feignChrome
import com.muedsa.tvbox.tool.get
import com.muedsa.tvbox.tool.parseHtml
import com.muedsa.tvbox.tool.toRequestBuild
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import timber.log.Timber
import java.util.StringJoiner

class MediaDetailService(
    private val libVioService: LibVioService,
    private val okHttpClient: OkHttpClient
) : IMediaDetailService {

    override suspend fun getDetailData(mediaId: String, detailUrl: String): MediaDetail {
        val body = "${libVioService.getSiteUrl()}$detailUrl".toRequestBuild()
            .feignChrome()
            .get(okHttpClient = okHttpClient)
            .checkSuccess()
            .parseHtml()
            .body()
        val contentEl =
            body.selectFirst(".container .row .stui-pannel .stui-pannel-box .stui-content")
                ?: throw RuntimeException("解析视频详情失败 $detailUrl")
        val imgUrl = contentEl.selectFirst(".stui-content__thumb img")
            ?.attr("data-original")
            ?: throw RuntimeException("解析视频详情失败 img $detailUrl")
        val title = contentEl.selectFirst(".stui-content__detail h1")?.text()?.trim()
            ?: throw RuntimeException("解析视频详情失败 title $detailUrl")
        val detailJoiner = StringJoiner("\n")
        contentEl.select(".stui-content__detail .data").map {
            detailJoiner.add(it.text().trim())
        }
        contentEl.selectFirst(".stui-content__detail .detail .detail-content")?.let {
            detailJoiner.add("简介：${it.text().trim()}")
        }
        val playSourceList =
            body.select(".container .row .stui-pannel .stui-pannel-box .stui-vodlist__head")
                .mapNotNull { vodListEl ->
                    val playSourceName =
                        vodListEl.selectFirst(".stui-pannel__head h3")?.text()?.trim()
                    if (playSourceName.isNullOrEmpty() || playSourceName.startsWith("视频下载")) {
                        return@mapNotNull null
                    }
                    val episodeList = vodListEl.select(".stui-content__playlist li")
                        .mapNotNull { liEl ->
                            val aEL = liEl.selectFirst("a[href]")
                            if (aEL != null) {
                                val eName = aEL.text().trim()
                                MediaEpisode(
                                    id = eName,
                                    name = eName,
                                    flag5 = aEL.attr("href")
                                )
                            } else null
                        }
                    if (episodeList.isNotEmpty()) {
                        MediaPlaySource(
                            id = playSourceName,
                            name = playSourceName,
                            episodeList = episodeList
                        )
                    } else null
                }
        val cardList =
            body.select(".container .row .stui-pannel .stui-pannel-box .stui-vodlist li")
                .mapNotNull { liEl ->
                    val thumbEl = liEl.selectFirst(".stui-vodlist__box .stui-vodlist__thumb")
                    val titleEl = liEl.selectFirst(".stui-vodlist__detail h4")
                    if (titleEl != null && thumbEl != null) {
                        val dUrl = thumbEl.attr("href")
                        MediaCard(
                            id = dUrl,
                            title = titleEl.text().trim(),
                            detailUrl = dUrl,
                            subTitle = thumbEl.selectFirst(".text-right")?.text()?.trim(),
                            coverImageUrl = thumbEl.attr("data-original")
                        )
                    } else null
                }
        return MediaDetail(
            id = mediaId,
            title = title,
            subTitle = null,
            description = detailJoiner.toString(),
            detailUrl = detailUrl,
            backgroundImageUrl = imgUrl,
            playSourceList = playSourceList,
            favoritedMediaCard = SavedMediaCard(
                id = mediaId,
                title = title,
                detailUrl = detailUrl,
                coverImageUrl = imgUrl,
                cardWidth = LibVidConst.CARD_WIDTH,
                cardHeight = LibVidConst.CARD_HEIGHT
            ),
            rows = if (cardList.isNotEmpty()) listOf(
                MediaCardRow(
                    title = "猜你喜欢",
                    list = cardList,
                    cardWidth = LibVidConst.CARD_WIDTH,
                    cardHeight = LibVidConst.CARD_HEIGHT
                )
            ) else emptyList()
        )
    }

    override suspend fun getEpisodePlayInfo(
        playSource: MediaPlaySource,
        episode: MediaEpisode
    ): MediaHttpSource {
        val playPageUrl =
            libVioService.getSiteUrl() + (episode.flag5 ?: throw RuntimeException("播放源地址为空"))
        val body = playPageUrl.toRequestBuild()
            .feignChrome()
            .get(okHttpClient = okHttpClient)
            .checkSuccess()
            .parseHtml()
            .body()
        val playerAAAAJson = PLAYER_AAAA_REGEX.find(body.html())?.groups?.get(1)?.value
            ?: throw RuntimeException("解析播放源地址失败 player_aaaa")
        var playerAAAA = LenientJson.decodeFromString<PlayerAAAA>(playerAAAAJson)
        if (playerAAAA.encrypt == 2) {
            playerAAAA = playerAAAA.copy(
                url = playerAAAA.url.decodeBase64ToStr(),
                urlNext = playerAAAA.urlNext.decodeBase64ToStr()
            )
        }
        Timber.i("playerAAAA = $playerAAAA")
        var url = PLAYER_URL_MAP[playerAAAA.from]
            ?: throw RuntimeException("解析地址失败, 不支持的播放源")
        url = url.replace("{url}", playerAAAA.url)
            .replace("{next}", playerAAAA.urlNext)
            .replace("{id}", playerAAAA.id)
            .replace("{nid}", "${playerAAAA.nid}")
        if (url.startsWith("/vid/")) {
            delay(200)
            return parseVidFromUrl(
                url = "${libVioService.getSiteUrl()}${url}",
                referrer = playPageUrl,
            )
        }
        if (playerAAAA.url.startsWith("http")
            && (playerAAAA.url.endsWith(".mp4", true)
                    || playerAAAA.url.endsWith(".mp4", true))) {
            return MediaHttpSource(
                url = playerAAAA.url,
                httpHeaders = null,
            )
        }
        if (url.startsWith("/static/")) {
            throw RuntimeException("解析地址失败, 不支持的播放源")
        }
        throw RuntimeException("解析地址失败, 不支持的播放源")
    }

    private fun parseVidFromUrl(
        url: String,
        referrer: String,
    ): MediaHttpSource {
        val doc = url.toRequestBuild()
            .feignChrome(referer = referrer)
            .get(okHttpClient = okHttpClient)
            .checkSuccess()
            .parseHtml()
        val head = doc.head()
        val body = doc.body()
        val vid = VID_REGEX.find(body.html())?.groups?.get(1)?.value
            ?: throw RuntimeException("解析播放源地址失败 vid")
        var mediaSourceWithReferrer = true
        head.selectFirst("meta[name=\"referrer\"]")?.let {
            mediaSourceWithReferrer = it.attr("content") != "no-referrer"
        }
        return MediaHttpSource(
            url = vid,
            httpHeaders = if (mediaSourceWithReferrer) mapOf("Referer" to referrer) else null,
        )
    }

    override suspend fun getEpisodeDanmakuDataList(episode: MediaEpisode): List<DanmakuData> =
        emptyList()

    override suspend fun getEpisodeDanmakuDataFlow(episode: MediaEpisode): DanmakuDataFlow? = null

    companion object {
        val PLAYER_AAAA_REGEX =
            "<script type=\"text/javascript\">var player_aaaa=(\\{.*?\\})</script>".toRegex()
        val VID_REGEX = "var vid = '(.*?)';".toRegex()

        val PLAYER_URL_MAP = mapOf(
            "mux" to "/vid/plyr/index3.php?url={url}&next={next}&id={id}&nid={nid}",                            // BD2
            "vr2" to "/vid/plyr/vr2.php?url={url}&next={next}&id={id}&nid={nid}",                               // BD5
            "LINE405" to "/vid/plyr/?url={url}&next={next}&id={id}&nid={nid}",                                  // HD
            "yd189" to "/vid/yd.php?url={url}&next={next}&id={id}&nid={nid}",                                   // HD5
            "tweb" to "https://testtestbd1.chinaeast2.cloudapp.chinacloudapi.cn:9091{url}",                     // AI
            "aliyunline2" to "/vid/ty2.php?url={url}&next={next}&id={id}&nid={nid}",                            // BD4
            "ty_new1" to "/vid/ty4.php?url={url}&next={next}&id={id}&nid={nid}",                                // BD
            "aishare" to "https://p2.cfnode1.xyz/aishare.php?url={url}&next={next}&id={id}&nid={nid}",          // AI2
            "aitemp" to "https://testtestbd1.chinaeast2.cloudapp.chinacloudapi.cn:9091{url}",                   // AI
            "aliyunline3" to "/vid/ty3.php?url={url}&next={next}&id={id}&nid={nid}",                            // BD4
            "hd01" to "/vid/plyr/index2.php?url={url}&next={next}&id={id}&nid={nid}",                           // HD
            "LINE500" to "/vid/lb3.php?url={url}&next={next}&id={id}&nid={nid}",                                // HD3
            "LINE400" to "/vid/lb2.php?url={url}&next={next}&id={id}&nid={nid}",                                // HD2
            "aliyunline" to "/vid/ty.php?url={url}&next={next}&id={id}&nid={nid}",                              // BD
            "aliyunpan" to "https://cbsh-d0145678.chinaeast2.cloudapp.chinacloudapi.cn:1301/watch?url={url}&next={next}&id={id}&nid={nid}",   // AI
            "tianyi" to "/vid/ty.php?url={url}&next={next}&id={id}&nid={nid}",                                  // BD3
            "tianyi_625" to "/vid/ty.php?url={url}&next={next}&id={id}&nid={nid}",                              // BD3
            "ali" to "https://p.cfnode1.xyz/ai.php?url={url}&next={next}&id={id}&nid={nid}",                    // AI
            "LINE407" to "/vid/lb2.php?url={url}&next={next}&id={id}&nid={nid}",                                // LINE400
            "LINE408" to "https://sh-data-s01.chinaeast2.cloudapp.chinacloudapi.cn/ck.php?url={url}&next={next}&id={id}&nid={nid}", // LINE408
            "p301" to "/static/img/line400.html",                                                               // HD6
            "p300" to "/static/img/line400.html?v=1.1",                                                         // LINE300
            "line402-日语" to "/static/img/line400.html",                                                        // LINE402
            "line401" to "/static/img/line400.html",                                                            // LINE401
//            "iframe296" to "",
//            "iframe297" to "",
//            "iframe307" to "",
//            "iframe308" to "",
//            "iframe309" to "",
            "line301" to "",
            "line302" to "",
            "banquan" to "/static/player/banquan.html",                                                                                    // 版权下架
//            "iframe268" to "",
//            "iframe290" to "",
//            "iframe291" to "",
            "LINE409" to "https://sh-data-s01.chinaeast2.cloudapp.chinacloudapi.cn/zm.php?url={url}&next={next}&id={id}&nid={nid}",
//            "iframe261" to "",
//            "iframe265" to "",
//            "iframe278" to "",
//            "iframe306" to "",
            "LINE406" to "https://sh-data-s01.chinaeast2.cloudapp.chinacloudapi.cn/zm.php?url={url}&next={next}&id={id}&nid={nid}",
            "dplayer3" to "/static/img/load.html",
            "dplayer2" to "/static/img/load.html",
            "dplayer" to "/static/img/load.html",
            "uc" to "/static/player/uc.html",
            "kuake" to "/static/player/kuake.html",
            "xunlei2" to "/static/player/xunlei2.html",
            "xunlei1" to "/static/player/xunlei.html",
            "yc" to "",
            "LINE1080" to "https://sh-data-s01.chinaeast2.cloudapp.chinacloudapi.cn/?url={url}&next={next}&id={id}&nid={nid}",
            "duoduozy" to "",
            "yr" to "https://sh-data-s01.chinaeast2.cloudapp.chinacloudapi.cn/yuer/yr.php?url={url}&next={next}&id={id}&nid={nid}",
            "app" to "/static/player/app.html",
            "zanwu" to "/static/player/zanwu.html",
        )
    }
}