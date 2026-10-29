package com.muedsa.tvbox.libvio

import com.muedsa.tvbox.libvio.service.LibVioPoWChallengeService
import com.muedsa.tvbox.libvio.service.LibVioService
import com.muedsa.tvbox.libvio.service.MainScreenService
import com.muedsa.tvbox.libvio.service.MediaCatalogService
import com.muedsa.tvbox.libvio.service.MediaDetailService
import com.muedsa.tvbox.libvio.service.MediaSearchService
import com.muedsa.tvbox.tool.PluginCookieJar
import com.muedsa.tvbox.tool.SharedCookieSaver
import com.muedsa.tvbox.tool.createOkHttpClient
import java.util.concurrent.TimeUnit

val TestStore by lazy {
    FakePluginPrefStore()
}

val TestCookieSaver by lazy { SharedCookieSaver(store = TestStore) }

val TestOkHttpClient by lazy {
    createOkHttpClient(
        debug = true,
        cookieJar = PluginCookieJar(
            saver = TestCookieSaver
        ),
        onlyIpv4 = false,
    ) {
        callTimeout(40, TimeUnit.SECONDS)
        readTimeout(60, TimeUnit.SECONDS)
    }
}

val TestLibVioPoWChallengeService by lazy {
    LibVioPoWChallengeService(
        cookieSaver = TestCookieSaver,
    )
}

val TestLibVioService by lazy {
    LibVioService(
        libVioPoWChallengeService = TestLibVioPoWChallengeService,
        okHttpClient = TestOkHttpClient,
    )
}

val TestMainScreenService by lazy {
    MainScreenService(
        libVioService = TestLibVioService,
        okHttpClient = TestOkHttpClient,
    )
}

val TestMediaDetailService by lazy {
    MediaDetailService(
        libVioService = TestLibVioService,
        okHttpClient = TestOkHttpClient,
    )
}

val TestMediaSearchService by lazy {
    MediaSearchService(
        libVioService = TestLibVioService,
        okHttpClient = TestOkHttpClient,
    )
}

val TestMediaCatalogService by lazy {
    MediaCatalogService(
        libVioService = TestLibVioService,
        okHttpClient = TestOkHttpClient,
    )
}