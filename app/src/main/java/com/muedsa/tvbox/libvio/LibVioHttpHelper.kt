package com.muedsa.tvbox.libvio

import com.muedsa.tvbox.tool.feignChrome
import okhttp3.Request

fun Request.Builder.feignChrome2(referer: String? = null) = this.feignChrome(referer = referer)
    .header(
        "Accept",
        "text/html,application/xhtml+xml,application/xml;q=0.9,image/jxl,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"
    )
