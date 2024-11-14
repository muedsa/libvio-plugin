package com.muedsa.tvbox.libvio.service

import com.muedsa.tvbox.api.data.MediaCard
import com.muedsa.tvbox.api.data.MediaCardRow
import com.muedsa.tvbox.api.service.IMainScreenService
import com.muedsa.tvbox.libvio.LibVidConst
import com.muedsa.tvbox.tool.checkSuccess
import com.muedsa.tvbox.tool.feignChrome
import com.muedsa.tvbox.tool.get
import com.muedsa.tvbox.tool.parseHtml
import com.muedsa.tvbox.tool.toRequestBuild
import okhttp3.OkHttpClient
import org.jsoup.nodes.Element

class MainScreenService(
    private val libVioService: LibVioService,
    private val okHttpClient: OkHttpClient
) : IMainScreenService {

    override suspend fun getRowsData(): List<MediaCardRow> {
        val body = "${libVioService.getSiteUrl()}/".toRequestBuild()
            .feignChrome()
            .get(okHttpClient = okHttpClient)
            .checkSuccess()
            .parseHtml()
            .body()
        val bdEl = body.selectFirst(".container .row .stui-pannel .stui-pannel__hd .container .stui-pannel__bd")
            ?: throw RuntimeException("解析首页失败")
        val vodListEls = bdEl.select(">.stui-vodlist")
        val vodListHeadEls = bdEl.select(">.stui-vodlist__head")
        if (vodListEls.isEmpty()) {
            return emptyList()
        }
        val rows = mutableListOf<MediaCardRow>()
        rows.add(
            MediaCardRow(
                title = "最近更新",
                list = parseVodList(vodListEls[0]),
                cardWidth = LibVidConst.CARD_WIDTH,
                cardHeight = LibVidConst.CARD_HEIGHT
            )
        )
        vodListHeadEls.forEachIndexed { index, headEls ->
            if (index + 1 < vodListEls.size) {
                rows.add(
                    MediaCardRow(
                        title = headEls.selectFirst("h3")?.text()?.trim() ?: "(╯°□°）╯︵ ┻━┻",
                        list = parseVodList(vodListEls[index + 1]),
                        cardWidth = LibVidConst.CARD_WIDTH,
                        cardHeight = LibVidConst.CARD_HEIGHT
                    )
                )
            }
        }
        return rows
    }

    private fun parseVodList(vodListEl: Element): List<MediaCard> {
        return vodListEl.select("li .stui-vodlist__box").mapNotNull {
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
    }
}