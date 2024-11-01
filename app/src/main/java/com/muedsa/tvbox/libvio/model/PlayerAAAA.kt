package com.muedsa.tvbox.libvio.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlayerAAAA(
    @SerialName("flag") val flag: String = "",
    @SerialName("encrypt") val encrypt: Int = Int.MIN_VALUE,
    @SerialName("trysee") val trySee: Int = Int.MIN_VALUE,
    @SerialName("points") val points: Int = Int.MIN_VALUE,
    @SerialName("link") val link: String = "",
    @SerialName("link_next") val linkNext: String = "",
    @SerialName("link_pre") val linkPre: String = "",
    @SerialName("url") val url: String = "",
    @SerialName("url_next") val urlNext: String = "",
    @SerialName("from") val from: String = "",
    @SerialName("server") val server: String = "",
    @SerialName("note") val note: String = "",
    @SerialName("id") val id: String = "",
    @SerialName("sid") val sid: Int = Int.MIN_VALUE,
    @SerialName("nid") val nid: Int = Int.MIN_VALUE
)
