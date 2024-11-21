package com.muedsa.tvbox.libvio.service

import com.muedsa.tvbox.api.data.MediaCatalogOption
import com.muedsa.tvbox.libvio.TestPlugin
import com.muedsa.tvbox.libvio.checkMediaCard
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MediaCatalogServiceTest {

    private val service = TestPlugin.provideMediaCatalogService()

    @Test
    fun getConfig_test() = runTest {
        val config = service.getConfig()
        check(config.initKey == "1")
        check(config.pageSize > 0)
        check(config.cardWidth > 0)
        check(config.cardHeight > 0)
        check(config.catalogOptions.isNotEmpty())
    }

    @Test
    fun catalog_test() = runTest {
        val config = service.getConfig()
        val options = MediaCatalogOption.getDefault(config.catalogOptions)
        val result =
            service.catalog(options = options, loadKey = config.initKey, loadSize = config.pageSize)
        check(result.list.isNotEmpty())
        result.list.forEach { card -> checkMediaCard(card = card, cardType = config.cardType) }
        check(result.prevKey == null)
        check(result.nextKey == "2")
    }
}