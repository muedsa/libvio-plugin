package com.muedsa.tvbox.libvio

import com.muedsa.tvbox.api.data.MediaCatalogOption
import com.muedsa.tvbox.api.data.MediaCatalogOptionItem
import java.util.Calendar

object LibVidConst {
    const val CARD_WIDTH = 124
    const val CARD_HEIGHT = 186

    val CATALOG_OPTIONS = listOf(
        MediaCatalogOption(
            name = "类型",
            value = "category",
            required = true,
            items = listOf(
                MediaCatalogOptionItem(
                    name = "电影",
                    value = "1",
                    defaultChecked = true
                ),
                MediaCatalogOptionItem(
                    name = "剧集",
                    value = "2",
                ),
                MediaCatalogOptionItem(
                    name = "动漫",
                    value = "3",
                ),
                MediaCatalogOptionItem(
                    name = "日韩剧",
                    value = "15",
                ),
                MediaCatalogOptionItem(
                    name = "欧美剧",
                    value = "16",
                ),
            )
        ),
        MediaCatalogOption(
            name = "剧情",
            value = "genre",
            required = true,
            items = listOf(
                MediaCatalogOptionItem(
                    name = "全部",
                    value = "",
                    defaultChecked = true,
                ),
                MediaCatalogOptionItem(
                    name = "喜剧",
                    value = "喜剧",
                ),
                MediaCatalogOptionItem(
                    name = "爱情",
                    value = "爱情",
                ),
                MediaCatalogOptionItem(
                    name = "恐怖",
                    value = "恐怖",
                ),
                MediaCatalogOptionItem(
                    name = "动作",
                    value = "动作",
                ),
                MediaCatalogOptionItem(
                    name = "科幻",
                    value = "科幻",
                ),
                MediaCatalogOptionItem(
                    name = "剧情",
                    value = "剧情",
                ),
                MediaCatalogOptionItem(
                    name = "战争",
                    value = "战争",
                ),
                MediaCatalogOptionItem(
                    name = "警匪",
                    value = "警匪",
                ),
                MediaCatalogOptionItem(
                    name = "犯罪",
                    value = "犯罪",
                ),
                MediaCatalogOptionItem(
                    name = "动画",
                    value = "动画",
                ),
                MediaCatalogOptionItem(
                    name = "奇幻",
                    value = "奇幻",
                ),
                MediaCatalogOptionItem(
                    name = "武侠",
                    value = "武侠",
                ),
                MediaCatalogOptionItem(
                    name = "冒险",
                    value = "冒险",
                ),
                MediaCatalogOptionItem(
                    name = "古装",
                    value = "古装",
                ),
                MediaCatalogOptionItem(
                    name = "青春",
                    value = "青春",
                ),
                MediaCatalogOptionItem(
                    name = "偶像",
                    value = "偶像",
                ),
                MediaCatalogOptionItem(
                    name = "家庭",
                    value = "家庭",
                ),
                MediaCatalogOptionItem(
                    name = "剧情",
                    value = "剧情",
                ),
                MediaCatalogOptionItem(
                    name = "历史",
                    value = "历史",
                ),
                MediaCatalogOptionItem(
                    name = "经典",
                    value = "经典",
                ),
                MediaCatalogOptionItem(
                    name = "情景",
                    value = "情景",
                ),
                MediaCatalogOptionItem(
                    name = "情感",
                    value = "情感",
                ),
                MediaCatalogOptionItem(
                    name = "热血",
                    value = "热血",
                ),
                MediaCatalogOptionItem(
                    name = "推理",
                    value = "推理",
                ),
                MediaCatalogOptionItem(
                    name = "搞笑",
                    value = "搞笑",
                ),
                MediaCatalogOptionItem(
                    name = "萝莉",
                    value = "萝莉",
                ),
                MediaCatalogOptionItem(
                    name = "校园",
                    value = "校园",
                ),
                MediaCatalogOptionItem(
                    name = "机战",
                    value = "机战",
                ),
                MediaCatalogOptionItem(
                    name = "运动",
                    value = "运动",
                ),
                MediaCatalogOptionItem(
                    name = "少年",
                    value = "少年",
                ),
            )
        ),
        MediaCatalogOption(
            name = "地区",
            value = "region",
            items = listOf(
                MediaCatalogOptionItem(
                    name = "全部",
                    value = "",
                    defaultChecked = true,
                ),
                MediaCatalogOptionItem(
                    name = "中国大陆",
                    value = "中国大陆",
                ),
                MediaCatalogOptionItem(
                    name = "中国香港",
                    value = "中国香港",
                ),
                MediaCatalogOptionItem(
                    name = "中国台湾",
                    value = "中国台湾",
                ),
                MediaCatalogOptionItem(
                    name = "美国",
                    value = "美国",
                ),
                MediaCatalogOptionItem(
                    name = "法国",
                    value = "法国",
                ),
                MediaCatalogOptionItem(
                    name = "英国",
                    value = "英国",
                ),
                MediaCatalogOptionItem(
                    name = "日本",
                    value = "日本",
                ),
                MediaCatalogOptionItem(
                    name = "日本",
                    value = "日本",
                ),
                MediaCatalogOptionItem(
                    name = "韩国",
                    value = "韩国",
                ),
                MediaCatalogOptionItem(
                    name = "德国",
                    value = "德国",
                ),
                MediaCatalogOptionItem(
                    name = "泰国",
                    value = "泰国",
                ),
                MediaCatalogOptionItem(
                    name = "印度",
                    value = "印度",
                ),
                MediaCatalogOptionItem(
                    name = "意大利",
                    value = "意大利",
                ),
                MediaCatalogOptionItem(
                    name = "西班牙",
                    value = "西班牙",
                ),
            )
        ),
        MediaCatalogOption(
            name = "年份",
            value = "year",
            required = true,
            items = buildList {
                add(
                    MediaCatalogOptionItem(
                        name = "全部",
                        value = "",
                        defaultChecked = true,
                    )
                )
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                for (year in 1998..currentYear) {
                    MediaCatalogOptionItem(
                        name = "$year",
                        value = "$year",
                        defaultChecked = true,
                    )
                }
            }
        ),
        MediaCatalogOption(
            name = "语言",
            value = "language",
            required = true,
            items = listOf(
                MediaCatalogOptionItem(
                    name = "全部",
                    value = "",
                    defaultChecked = true
                ),
                MediaCatalogOptionItem(
                    name = "国语",
                    value = "国语",
                ),
                MediaCatalogOptionItem(
                    name = "英语",
                    value = "英语",
                ),
                MediaCatalogOptionItem(
                    name = "粤语",
                    value = "粤语",
                ),
                MediaCatalogOptionItem(
                    name = "闽南语",
                    value = "闽南语",
                ),
                MediaCatalogOptionItem(
                    name = "韩语",
                    value = "韩语",
                ),
                MediaCatalogOptionItem(
                    name = "日语",
                    value = "日语",
                ),
                MediaCatalogOptionItem(
                    name = "法语",
                    value = "法语",
                ),
                MediaCatalogOptionItem(
                    name = "德语",
                    value = "德语",
                ),
                MediaCatalogOptionItem(
                    name = "其它",
                    value = "其它",
                ),
            )
        ),
        MediaCatalogOption(
            name = "排序",
            value = "order",
            required = true,
            items = listOf(
                MediaCatalogOptionItem(
                    name = "时间",
                    value = "time",
                    defaultChecked = true
                ),
                MediaCatalogOptionItem(
                    name = "人气",
                    value = "hits",
                ),
                MediaCatalogOptionItem(
                    name = "评分",
                    value = "score",
                ),
            )
        ),
    )
}