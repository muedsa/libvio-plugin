package com.muedsa.tvbox.libvio

import com.muedsa.tvbox.api.plugin.IPlugin
import com.muedsa.tvbox.api.plugin.PluginOptions
import com.muedsa.tvbox.api.plugin.TvBoxContext
import com.muedsa.tvbox.api.service.IMainScreenService
import com.muedsa.tvbox.api.service.IMediaDetailService
import com.muedsa.tvbox.api.service.IMediaSearchService
import com.muedsa.tvbox.libvio.service.LibVioService
import com.muedsa.tvbox.libvio.service.MainScreenService
import com.muedsa.tvbox.libvio.service.MediaDetailService
import com.muedsa.tvbox.libvio.service.MediaSearchService

class LibVioPlugin(tvBoxContext: TvBoxContext) : IPlugin(tvBoxContext = tvBoxContext) {

    override var options: PluginOptions = PluginOptions(enableDanDanPlaySearch = false)

    override suspend fun onInit() {}

    override suspend fun onLaunched() {}

    private val libVioService by lazy { LibVioService() }

    private val mainScreenService by lazy { MainScreenService(libVioService) }
    private val mediaDetailService by lazy { MediaDetailService(libVioService) }
    private val mediaSearchService by lazy { MediaSearchService(libVioService) }

    override fun provideMainScreenService(): IMainScreenService = mainScreenService

    override fun provideMediaDetailService(): IMediaDetailService = mediaDetailService

    override fun provideMediaSearchService(): IMediaSearchService = mediaSearchService
}