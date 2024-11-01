package com.muedsa.tvbox.libvio.service

import com.muedsa.tvbox.api.data.MediaCard
import com.muedsa.tvbox.api.data.MediaCardRow
import com.muedsa.tvbox.api.service.IMediaSearchService
import com.muedsa.tvbox.libvio.LibVidConst
import com.muedsa.tvbox.libvio.feignChrome
import org.jsoup.Jsoup

class MediaSearchService(
    private val libVioService: LibVioService
) : IMediaSearchService {

    override suspend fun searchMedias(query: String): MediaCardRow {
        val body =
            Jsoup.connect("${libVioService.getSiteUrl()}/search/-------------.html?wd=$query")
                .feignChrome()
                .get()
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