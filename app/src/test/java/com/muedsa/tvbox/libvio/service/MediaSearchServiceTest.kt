package com.muedsa.tvbox.libvio.service

import com.muedsa.tvbox.libvio.TestPlugin
import com.muedsa.tvbox.libvio.checkMediaCardRow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MediaSearchServiceTest {

    private val service = TestPlugin.provideMediaSearchService()

    @Test
    fun searchMedias_test() = runTest {
        val row = service.searchMedias("GIRLS BAND CRY")
        checkMediaCardRow(row = row)
    }
}