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
import okhttp3.HttpUrl.Companion.toHttpUrl
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
        val parseFun = parseFunctionMap[playerAAAA.from]
            ?: throw RuntimeException("解析地址失败, 不支持的播放源")
        delay(200)
        return parseFun(playerAAAA, playPageUrl)
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

    private val parseFunctionMap = mapOf<String, suspend (PlayerAAAA, String) -> MediaHttpSource>(
        "vr2" to { playerAAAA, referrer ->
            // src="/vid/plyr/vr2.php?url='+MacPlayer.PlayUrl+'&next='+MacPlayer.PlayLinkNext+'&id='+MacPlayer.Id+'&nid='+MacPlayer.Nid+'"
            parseVidFromUrl(
                url = "${libVioService.getSiteUrl()}/vid/plyr/vr2.php".toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("url", playerAAAA.url)
                    .addQueryParameter("next", playerAAAA.urlNext)
                    .addQueryParameter("id", playerAAAA.id)
                    .addQueryParameter("nid", playerAAAA.nid.toString())
                    .build()
                    .toString(),
                referrer = referrer
            )
        },
        "LINE405" to { playerAAAA, referrer ->
            // src="/vid/plyr/?url='+MacPlayer.PlayUrl+'&next='+MacPlayer.PlayLinkNext+'&id='+MacPlayer.Id+'&nid='+MacPlayer.Nid+'"
            parseVidFromUrl(
                url = "${libVioService.getSiteUrl()}/vid/plyr/".toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("url", playerAAAA.url)
                    .addQueryParameter("next", playerAAAA.urlNext)
                    .addQueryParameter("id", playerAAAA.id)
                    .addQueryParameter("nid", playerAAAA.nid.toString())
                    .build()
                    .toString(),
                referrer = referrer
            )
        },
        "hd01" to { playerAAAA, referrer ->
            // src="/vid/plyr/index2.php?url='+MacPlayer.PlayUrl+'&next='+MacPlayer.PlayLinkNext+'&id='+MacPlayer.Id+'&nid='+MacPlayer.Nid+'"
            parseVidFromUrl(
                url = "${libVioService.getSiteUrl()}/vid/plyr/index2.php".toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("url", playerAAAA.url)
                    .addQueryParameter("next", playerAAAA.urlNext)
                    .addQueryParameter("id", playerAAAA.id)
                    .addQueryParameter("nid", playerAAAA.nid.toString())
                    .build()
                    .toString(),
                referrer = referrer
            )
        },
        "mux" to { playerAAAA, referrer ->
            // src="/vid/plyr/index3.php?url='+MacPlayer.PlayUrl+'&next='+MacPlayer.PlayLinkNext+'&id='+MacPlayer.Id+'&nid='+MacPlayer.Nid+'"
            parseVidFromUrl(
                url = "${libVioService.getSiteUrl()}/vid/plyr/index3.php".toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("url", playerAAAA.url)
                    .addQueryParameter("next", playerAAAA.urlNext)
                    .addQueryParameter("id", playerAAAA.id)
                    .addQueryParameter("nid", playerAAAA.nid.toString())
                    .build()
                    .toString(),
                referrer = referrer
            )
        },
        "yd189" to { playerAAAA, referrer ->
            // src="/vid/yd.php?url='+MacPlayer.PlayUrl+'&next='+MacPlayer.PlayLinkNext+'&id='+MacPlayer.Id+'&nid='+MacPlayer.Nid+'"
            parseVidFromUrl(
                url = "${libVioService.getSiteUrl()}/vid/yd.php".toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("url", playerAAAA.url)
                    .addQueryParameter("next", playerAAAA.urlNext)
                    .addQueryParameter("id", playerAAAA.id)
                    .addQueryParameter("nid", playerAAAA.nid.toString())
                    .build()
                    .toString(),
                referrer = referrer
            )
        },
        "aliyunline" to { playerAAAA, referrer ->
            // src="/vid/ty.php?url='+MacPlayer.PlayUrl+'&next='+MacPlayer.PlayLinkNext+'&id='+MacPlayer.Id+'&nid='+MacPlayer.Nid+'"
            parseVidFromUrl(
                url = "${libVioService.getSiteUrl()}/vid/ty.php".toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("url", playerAAAA.url)
                    .addQueryParameter("next", playerAAAA.urlNext)
                    .addQueryParameter("id", playerAAAA.id)
                    .addQueryParameter("nid", playerAAAA.nid.toString())
                    .build()
                    .toString(),
                referrer = referrer
            )
        },
        "tianyi" to { playerAAAA, referrer ->
            // src="/vid/ty.php?url='+MacPlayer.PlayUrl+'&next='+MacPlayer.PlayLinkNext+'&id='+MacPlayer.Id+'&nid='+MacPlayer.Nid+'"
            parseVidFromUrl(
                url = "${libVioService.getSiteUrl()}/vid/ty.php".toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("url", playerAAAA.url)
                    .addQueryParameter("next", playerAAAA.urlNext)
                    .addQueryParameter("id", playerAAAA.id)
                    .addQueryParameter("nid", playerAAAA.nid.toString())
                    .build()
                    .toString(),
                referrer = referrer
            )
        },
        "tianyi_625" to { playerAAAA, referrer ->
            // src="/vid/ty.php?url='+MacPlayer.PlayUrl+'&next='+MacPlayer.PlayLinkNext+'&id='+MacPlayer.Id+'&nid='+MacPlayer.Nid+'"
            parseVidFromUrl(
                url = "${libVioService.getSiteUrl()}/vid/ty.php".toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("url", playerAAAA.url)
                    .addQueryParameter("next", playerAAAA.urlNext)
                    .addQueryParameter("id", playerAAAA.id)
                    .addQueryParameter("nid", playerAAAA.nid.toString())
                    .build()
                    .toString(),
                referrer = referrer
            )
        },
        "aliyunline2" to { playerAAAA, referrer ->
            // src="/vid/ty2.php?url='+MacPlayer.PlayUrl+'&next='+MacPlayer.PlayLinkNext+'&id='+MacPlayer.Id+'&nid='+MacPlayer.Nid+'"
            parseVidFromUrl(
                url = "${libVioService.getSiteUrl()}/vid/ty2.php".toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("url", playerAAAA.url)
                    .addQueryParameter("next", playerAAAA.urlNext)
                    .addQueryParameter("id", playerAAAA.id)
                    .addQueryParameter("nid", playerAAAA.nid.toString())
                    .build()
                    .toString(),
                referrer = referrer
            )
        },
        "aliyunline3" to { playerAAAA, referrer ->
            // src="/vid/ty3.php?url='+MacPlayer.PlayUrl+'&next='+MacPlayer.PlayLinkNext+'&id='+MacPlayer.Id+'&nid='+MacPlayer.Nid+'"
            parseVidFromUrl(
                url = "${libVioService.getSiteUrl()}/vid/ty3.php".toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("url", playerAAAA.url)
                    .addQueryParameter("next", playerAAAA.urlNext)
                    .addQueryParameter("id", playerAAAA.id)
                    .addQueryParameter("nid", playerAAAA.nid.toString())
                    .build()
                    .toString(),
                referrer = referrer
            )
        },
        "ty_new1" to { playerAAAA, referrer ->
            //  src="/vid/ty4.php?url='+MacPlayer.PlayUrl+'&next='+MacPlayer.PlayLinkNext+'&id='+MacPlayer.Id+'&nid='+MacPlayer.Nid+'"
            parseVidFromUrl(
                url = "${libVioService.getSiteUrl()}/vid/ty4.php".toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("url", playerAAAA.url)
                    .addQueryParameter("next", playerAAAA.urlNext)
                    .addQueryParameter("id", playerAAAA.id)
                    .addQueryParameter("nid", playerAAAA.nid.toString())
                    .build()
                    .toString(),
                referrer = referrer
            )
        },
        "LINE400" to { playerAAAA, referrer ->
            // src="/vid/lb2.php?url='+MacPlayer.PlayUrl+'&next='+MacPlayer.PlayLinkNext+'&id='+MacPlayer.Id+'&nid='+MacPlayer.Nid+'"
            parseVidFromUrl(
                url = "${libVioService.getSiteUrl()}/vid/lb2.php".toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("url", playerAAAA.url)
                    .addQueryParameter("next", playerAAAA.urlNext)
                    .addQueryParameter("id", playerAAAA.id)
                    .addQueryParameter("nid", playerAAAA.nid.toString())
                    .build()
                    .toString(),
                referrer = referrer
            )
        },
        "LINE407" to { playerAAAA, referrer ->
            // src="/vid/lb2.php?url='+MacPlayer.PlayUrl+'&next='+MacPlayer.PlayLinkNext+'&id='+MacPlayer.Id+'&nid='+MacPlayer.Nid+'"
            parseVidFromUrl(
                url = "${libVioService.getSiteUrl()}/vid/lb2.php".toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("url", playerAAAA.url)
                    .addQueryParameter("next", playerAAAA.urlNext)
                    .addQueryParameter("id", playerAAAA.id)
                    .addQueryParameter("nid", playerAAAA.nid.toString())
                    .build()
                    .toString(),
                referrer = referrer
            )
        },
        "LINE500" to { playerAAAA, referrer ->
            // src="/vid/lb3.php?url='+MacPlayer.PlayUrl+'&next='+MacPlayer.PlayLinkNext+'&id='+MacPlayer.Id+'&nid='+MacPlayer.Nid+'"
            parseVidFromUrl(
                url = "${libVioService.getSiteUrl()}/vid/lb3.php".toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("url", playerAAAA.url)
                    .addQueryParameter("next", playerAAAA.urlNext)
                    .addQueryParameter("id", playerAAAA.id)
                    .addQueryParameter("nid", playerAAAA.nid.toString())
                    .build()
                    .toString(),
                referrer = referrer
            )
        },
        "tweb" to { playerAAAA, referrer ->
            // src="https://testtestbd1.chinaeast2.cloudapp.chinacloudapi.cn:9091'+MacPlayer.PlayUrl+'"
            throw RuntimeException("解析播放源地址失败, 不支持的播放源")
        },
        "aishare" to { playerAAAA, referrer ->
            // src="https://p2.cfnode1.xyz/aishare.php?url='+MacPlayer.PlayUrl+'&next='+MacPlayer.PlayLinkNext+'&id='+MacPlayer.Id+'&nid='+MacPlayer.Nid+'"
            throw RuntimeException("解析播放源地址失败, 不支持的播放源")
        },
        "aitemp" to { playerAAAA, referrer ->
            // src="https://testtestbd1.chinaeast2.cloudapp.chinacloudapi.cn:9091'+MacPlayer.PlayUrl+'"
            throw RuntimeException("解析播放源地址失败, 不支持的播放源")
        },
        "aliyunpan" to { playerAAAA, referrer ->
            // src="https://cbsh-d0145678.chinaeast2.cloudapp.chinacloudapi.cn:1301/watch?url='+MacPlayer.PlayUrl+'&next='+MacPlayer.PlayLinkNext+'&id='+MacPlayer.Id+'&nid='+MacPlayer.Nid+'"
            throw RuntimeException("解析播放源地址失败, 不支持的播放源")
        },
        "ali" to { playerAAAA, referrer ->
            // src="https://p.cfnode1.xyz/ai.php?url='+MacPlayer.PlayUrl+'&next='+MacPlayer.PlayLinkNext+'&id='+MacPlayer.Id+'&nid='+MacPlayer.Nid+'"
            throw RuntimeException("解析播放源地址失败, 不支持的播放源")
        },
        "LINE408" to { playerAAAA, referrer ->
            // src="https://sh-data-s01.chinaeast2.cloudapp.chinacloudapi.cn/ck.php?url='+MacPlayer.PlayUrl+'&next='+MacPlayer.PlayLinkNext+'&id='+MacPlayer.Id+'&nid='+MacPlayer.Nid+'"
            throw RuntimeException("解析播放源地址失败, 不支持的播放源")
        },
    )

    override suspend fun getEpisodeDanmakuDataList(episode: MediaEpisode): List<DanmakuData> =
        emptyList()

    override suspend fun getEpisodeDanmakuDataFlow(episode: MediaEpisode): DanmakuDataFlow? = null

    companion object {
        val PLAYER_AAAA_REGEX =
            "<script type=\"text/javascript\">var player_aaaa=(\\{.*?\\})</script>".toRegex()
        val VID_REGEX = "var vid = '(.*?)';".toRegex()
    }
}