package com.muedsa.tvbox.libvio.model

data class ChallengeParams(
    val ts: String,
    val sig: String,
    val diff: String,
    val mode: String
)