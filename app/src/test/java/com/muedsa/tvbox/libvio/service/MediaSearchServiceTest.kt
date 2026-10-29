package com.muedsa.tvbox.libvio.service

import com.muedsa.tvbox.libvio.TestMediaSearchService
import com.muedsa.tvbox.libvio.checkMediaCardRow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MediaSearchServiceTest {

    @Test
    fun searchMedias_test() = runTest {
        val row = TestMediaSearchService.searchMedias("GIRLS BAND CRY")
        checkMediaCardRow(row = row)
    }
}