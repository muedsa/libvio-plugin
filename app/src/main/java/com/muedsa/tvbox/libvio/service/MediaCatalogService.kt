package com.muedsa.tvbox.libvio.service

import com.muedsa.tvbox.api.data.MediaCard
import com.muedsa.tvbox.api.data.MediaCatalogConfig
import com.muedsa.tvbox.api.data.MediaCatalogOption
import com.muedsa.tvbox.api.data.PagingResult
import com.muedsa.tvbox.api.service.IMediaCatalogService
import com.muedsa.tvbox.libvio.LibVidConst
import com.muedsa.tvbox.tool.checkSuccess
import com.muedsa.tvbox.tool.feignChrome
import com.muedsa.tvbox.tool.get
import com.muedsa.tvbox.tool.parseHtml
import com.muedsa.tvbox.tool.toRequestBuild
import okhttp3.OkHttpClient

class MediaCatalogService(
    private val libVioService: LibVioService,
    private val okHttpClient: OkHttpClient,
) : IMediaCatalogService {

    override suspend fun getConfig(): MediaCatalogConfig =
        MediaCatalogConfig(
            initKey = "1",
            pageSize = 12,
            cardWidth = LibVidConst.CARD_WIDTH,
            cardHeight = LibVidConst.CARD_HEIGHT,
            catalogOptions = LibVidConst.CATALOG_OPTIONS
        )

    override suspend fun catalog(
        options: List<MediaCatalogOption>,
        loadKey: String,
        loadSize: Int
    ): PagingResult<MediaCard> {
        // 1-美国-hits-恐怖-英语----2---2024.html
        val page = loadKey.toInt()
        val category = options.findOptionFirstValue("category", defaultValue = "")
        val genre = options.findOptionFirstValue("genre", defaultValue = "")
        val region = options.findOptionFirstValue("region", defaultValue = "")
        val year = options.findOptionFirstValue("year", defaultValue = "")
        val language = options.findOptionFirstValue("language", defaultValue = "")
        val order = options.findOptionFirstValue("order", defaultValue = "")
        val body =
            "${libVioService.getSiteUrl()}/show/$category-$region-$order-$genre-$language----$page---$year.html"
                .toRequestBuild()
                .feignChrome()
                .get(okHttpClient = okHttpClient)
                .checkSuccess()
                .parseHtml()
                .body()
        val cards = body.select(".container .row .stui-pannel .stui-pannel__bd .stui-vodlist li").mapNotNull {
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
        }
        val pageLinkEls = body.select(".stui-pannel__ft >.stui-page__item li a")
        return PagingResult(
            list = cards,
            prevKey = pageLinkEls.find { it.text().trim().contains("上一页") }?.let {
                val p = urlToPage(it.attr("href"))
                if (p == loadKey) null else p
            },
            nextKey = pageLinkEls.find { it.text().contains("下一页") }?.let {
                val p = urlToPage(it.attr("href"))
                if (p == loadKey) null else p
            }
        )
    }

    companion object {
        val URL_PAGE_REGEX = "/show/\\w*-\\w*-\\w*-\\w*-\\w*-\\w*---(\\w*)---\\w*.html".toRegex()

        fun List<MediaCatalogOption>.findOptionFirstValue(
            optionValue: String,
            defaultValue: String
        ): String {
            val option = find { it.value == optionValue }
            return if (option != null && option.items.isNotEmpty()) {
                option.items[0].value
            } else defaultValue
        }

        fun urlToPage(url: String) = URL_PAGE_REGEX.find(url)?.groups[1]?.value ?: "1"
    }
}