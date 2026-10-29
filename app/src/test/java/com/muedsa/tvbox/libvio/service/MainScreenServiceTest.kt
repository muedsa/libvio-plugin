package com.muedsa.tvbox.libvio.service

import com.muedsa.tvbox.libvio.TestMainScreenService
import com.muedsa.tvbox.libvio.checkMediaCardRows
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MainScreenServiceTest {

    @Test
    fun getRowsDataTest() = runTest{
        val rows = TestMainScreenService.getRowsData()
        checkMediaCardRows(rows = rows)
    }

}