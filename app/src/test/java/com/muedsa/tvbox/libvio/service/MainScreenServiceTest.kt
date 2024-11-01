package com.muedsa.tvbox.libvio.service

import com.muedsa.tvbox.libvio.TestPlugin
import com.muedsa.tvbox.libvio.checkMediaCardRows
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MainScreenServiceTest {

    private val service = TestPlugin.provideMainScreenService()

    @Test
    fun getRowsDataTest() = runTest{
        val rows = service.getRowsData()
        checkMediaCardRows(rows = rows)
    }

}