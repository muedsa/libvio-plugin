package com.muedsa.tvbox.libvio.service

import com.muedsa.tvbox.api.data.MediaCard
import com.muedsa.tvbox.api.data.MediaCardRow
import com.muedsa.tvbox.api.data.MediaDetail
import com.muedsa.tvbox.api.data.MediaEpisode
import com.muedsa.tvbox.api.data.MediaHttpSource
import com.muedsa.tvbox.api.data.MediaPlaySource
import com.muedsa.tvbox.api.data.SavedMediaCard
import com.muedsa.tvbox.api.service.IMediaDetailService
import com.muedsa.tvbox.libvio.LibVidConst
import com.muedsa.tvbox.libvio.feignChrome
import com.muedsa.tvbox.libvio.model.PlayerAAAA
import com.muedsa.tvbox.tool.LenientJson
import com.muedsa.tvbox.tool.decodeBase64ToStr
import kotlinx.coroutines.delay
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.Jsoup
import java.util.StringJoiner

class MediaDetailService(
    private val libVioService: LibVioService
) : IMediaDetailService {

    override suspend fun getDetailData(mediaId: String, detailUrl: String): MediaDetail {
        val body = Jsoup.connect("${libVioService.getSiteUrl()}$detailUrl")
            .feignChrome()
            .get()
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
                    if (!playSourceName.isNullOrEmpty() && episodeList.isNotEmpty()) {
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
        val body = Jsoup.connect(playPageUrl)
            .feignChrome()
            .get()
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
        return step2(playerAAAA, referrer = playPageUrl)
    }

    private suspend fun step2(playerAAAA: PlayerAAAA, referrer: String): MediaHttpSource {
        delay(200)
        val url = "${libVioService.getSiteUrl()}/vid/plyr/vr2.php".toHttpUrl()
            .newBuilder()
            .addQueryParameter("url", playerAAAA.url)
            .addQueryParameter("next", playerAAAA.urlNext)
            .addQueryParameter("id", playerAAAA.id)
            .addQueryParameter("nid", playerAAAA.nid.toString())
            .build()
            .toString()
        val body = Jsoup.connect(url)
            .feignChrome(referrer)
            .get()
            .body()
        val vid = VID_REGEX.find(body.html())?.groups?.get(1)?.value
            ?: throw RuntimeException("解析播放源地址失败 vid")
        return MediaHttpSource(
            url = vid
        )
    }

    companion object {
        val PLAYER_AAAA_REGEX =
            "<script type=\"text/javascript\">var player_aaaa=(\\{.*?\\})</script>".toRegex()
        val VID_REGEX = "var vid = '(.*?)';".toRegex()
    }
}