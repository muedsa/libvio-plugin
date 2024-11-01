package com.muedsa.tvbox.libvio

import com.muedsa.tvbox.api.plugin.TvBoxContext

val TestPlugin by lazy {
    LibVioPlugin(
        tvBoxContext = TvBoxContext(
            screenWidth = 1920,
            screenHeight = 1080,
            debug = true,
            store = FakePluginPrefStore()
        )
    )
}