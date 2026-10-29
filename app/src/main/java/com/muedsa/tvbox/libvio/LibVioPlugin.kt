package com.muedsa.tvbox.libvio

import com.muedsa.tvbox.api.plugin.IPlugin
import com.muedsa.tvbox.api.plugin.PluginOptions
import com.muedsa.tvbox.api.plugin.TvBoxContext
import com.muedsa.tvbox.api.service.IMainScreenService
import com.muedsa.tvbox.api.service.IMediaCatalogService
import com.muedsa.tvbox.api.service.IMediaDetailService
import com.muedsa.tvbox.api.service.IMediaSearchService
import com.muedsa.tvbox.libvio.service.LibVioPoWChallengeService
import com.muedsa.tvbox.libvio.service.LibVioService
import com.muedsa.tvbox.libvio.service.MainScreenService
import com.muedsa.tvbox.libvio.service.MediaCatalogService
import com.muedsa.tvbox.libvio.service.MediaDetailService
import com.muedsa.tvbox.libvio.service.MediaSearchService
import com.muedsa.tvbox.tool.IPv6Checker
import com.muedsa.tvbox.tool.PluginCookieJar
import com.muedsa.tvbox.tool.SharedCookieSaver
import com.muedsa.tvbox.tool.createOkHttpClient
import java.util.concurrent.TimeUnit

class LibVioPlugin(tvBoxContext: TvBoxContext) : IPlugin(tvBoxContext = tvBoxContext) {

    override var options: PluginOptions = PluginOptions(enableDanDanPlaySearch = false)

    override suspend fun onInit() {}

    override suspend fun onLaunched() {}
    private val cookieSaver by lazy { SharedCookieSaver(store = tvBoxContext.store) }
    private val okHttpClient by lazy {
        createOkHttpClient(
            debug = tvBoxContext.debug,
            cookieJar = PluginCookieJar(
                saver = cookieSaver
            ),
            onlyIpv4 = tvBoxContext.iPv6Status != IPv6Checker.IPv6Status.SUPPORTED
        ) {
            callTimeout(40, TimeUnit.SECONDS)
            readTimeout(60, TimeUnit.SECONDS)
        }
    }
    private val libVioPoWChallengeService by lazy {
        LibVioPoWChallengeService(
            cookieSaver = cookieSaver,
        )
    }
    private val libVioService by lazy {
        LibVioService(
            libVioPoWChallengeService = libVioPoWChallengeService,
            okHttpClient = okHttpClient,
        )
    }
    private val mainScreenService by lazy {
        MainScreenService(
            libVioService = libVioService,
            okHttpClient = okHttpClient
        )
    }
    private val mediaDetailService by lazy {
        MediaDetailService(
            libVioService = libVioService,
            okHttpClient = okHttpClient
        )
    }
    private val mediaSearchService by lazy {
        MediaSearchService(
            libVioService = libVioService,
            okHttpClient = okHttpClient
        )
    }
    private val mediaCatalogService by lazy {
        MediaCatalogService(
            libVioService = libVioService,
            okHttpClient = okHttpClient
        )
    }

    override fun provideMainScreenService(): IMainScreenService = mainScreenService

    override fun provideMediaDetailService(): IMediaDetailService = mediaDetailService

    override fun provideMediaSearchService(): IMediaSearchService = mediaSearchService

    override fun provideMediaCatalogService(): IMediaCatalogService = mediaCatalogService
}