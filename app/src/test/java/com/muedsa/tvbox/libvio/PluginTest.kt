package com.muedsa.tvbox.libvio

import com.muedsa.tvbox.api.plugin.TvBoxContext
import com.muedsa.tvbox.tool.IPv6Checker
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PluginTest {

    private val testPlugin = LibVioPlugin(
        tvBoxContext = TvBoxContext(
            screenWidth = 1920,
            screenHeight = 1080,
            debug = true,
            store = FakePluginPrefStore(),
            iPv6Status = IPv6Checker.checkIPv6Support(),
        )
    )

    @Test
    fun onInit_test() = runTest {
        testPlugin.onInit()
    }

    @Test
    fun onLaunched_test() = runTest {
        testPlugin.onLaunched()
    }

    @Test
    fun provideMainScreenService_test() {
        testPlugin.provideMainScreenService()
    }

    @Test
    fun provideMediaDetailService_test() {
        testPlugin.provideMediaDetailService()
    }

    @Test
    fun provideMediaSearchService_test() {
        testPlugin.provideMediaSearchService()
    }
}