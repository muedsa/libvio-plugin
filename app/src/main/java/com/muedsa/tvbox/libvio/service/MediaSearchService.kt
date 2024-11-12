package com.muedsa.tvbox.libvio.service

import com.muedsa.tvbox.api.data.MediaCard
import com.muedsa.tvbox.api.data.MediaCardRow
import com.muedsa.tvbox.api.service.IMediaSearchService
import com.muedsa.tvbox.libvio.LibVidConst
import com.muedsa.tvbox.tool.feignChrome
import com.muedsa.tvbox.tool.get
import com.muedsa.tvbox.tool.parseHtml
import com.muedsa.tvbox.tool.toRequestBuild
import okhttp3.OkHttpClient

class MediaSearchService(
    private val libVioService: LibVioService,
    private val okHttpClient: OkHttpClient
) : IMediaSearchService {

    override suspend fun searchMedias(query: String): MediaCardRow {
        val body =
            "${libVioService.getSiteUrl()}/search/-------------.html?wd=$query".toRequestBuild()
                .feignChrome()
                .get(okHttpClient = okHttpClient)
                .parseHtml()
                .body()
        return MediaCardRow(
            title = "search list",
            list = body.select(".container >.stui-vodlist li .stui-vodlist__box").mapNotNull {
                val thumbEl = it.selectFirst(".stui-vodlist__box .stui-vodlist__thumb")
                val titleEl = it.selectFirst(".stui-vodlist__detail h4")
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
            },
            cardWidth = LibVidConst.CARD_WIDTH,
            cardHeight = LibVidConst.CARD_HEIGHT
        )
    }
}