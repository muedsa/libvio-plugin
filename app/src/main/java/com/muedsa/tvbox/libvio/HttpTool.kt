package com.muedsa.tvbox.libvio

import com.muedsa.tvbox.tool.ChromeUserAgent
import org.jsoup.Connection

fun Connection.feignChrome(referrer: String? = null): Connection {
    return userAgent(ChromeUserAgent)
        .also {
            if (!referrer.isNullOrEmpty()) {
                it.referrer(referrer)
            }
        }
        .header("Cache-Control", "no-cache")
        .header("Pragma", "no-cache")
        .header("Priority", "u=0, i")
        .header("Sec-Ch-Ua", "\"Chromium\";v=\"130\", \"Google Chrome\";v=\"130\", \"Not?A_Brand\";v=\"99\"")
        .header("Sec-Ch-Ua-Platform", "\"Windows\"")
        .header("Sec-Fetch-Dest", "document")
        .header("Sec-Fetch-Mode", "navigate")
        .header("Sec-Fetch-Site", "none")
        .header("Upgrade-Insecure-Requests", "1")
}