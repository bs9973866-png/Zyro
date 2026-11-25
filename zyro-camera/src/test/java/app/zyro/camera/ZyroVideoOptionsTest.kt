package app.zyro.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ZyroVideoOptionsTest {

    @Test
    fun availableQualitiesOnlyIncludesReportedResolutionTiers() {
        val camera = camera(
            sizes = listOf(ZyroVideoSize(1280, 720))
        )

        assertEquals(
            listOf(ZyroVideoQuality.SD, ZyroVideoQuality.HD, ZyroVideoQuality.MAX),
            ZyroVideoOptions.availableQualities(camera)
        )
    }

    @Test
    fun sizeForSelectsClosestSupportedLongEdge() {
        val camera = camera(
            sizes = listOf(
                ZyroVideoSize(1280, 720),
                ZyroVideoSize(1920, 1080),
                ZyroVideoSize(2560, 1440)
            )
        )

        assertEquals(
            ZyroVideoSize(1920, 1080),
            ZyroVideoOptions.sizeFor(camera, ZyroVideoQuality.FULL_HD)
        )
    }

    @Test
    fun availableFpsOnlyIncludesValuesInsideReportedAeRanges() {
        val camera = camera(
            ranges = listOf(ZyroFpsRange(24, 30), ZyroFpsRange(60, 60))
        )

        assertEquals(
            listOf(24, 25, 30, 60),
            ZyroVideoOptions.availableFps(camera)
        )
    }

    @Test
    fun emptyCameraMetadataReturnsNoModes() {
        val camera = camera(sizes = emptyList(), ranges = emptyList())

        assertEquals(emptyList<ZyroVideoQuality>(), ZyroVideoOptions.availableQualities(camera))
        assertEquals(emptyList<Int>(), ZyroVideoOptions.availableFps(camera))
        assertNull(ZyroVideoOptions.sizeFor(camera, ZyroVideoQuality.FULL_HD))
    }

    private fun camera(
        sizes: List<ZyroVideoSize> = listOf(ZyroVideoSize(1920, 1080)),
        ranges: List<ZyroFpsRange> = listOf(ZyroFpsRange(30, 30))
    ) = ZyroCameraInfo(
        cameraId = "0",
        lensFacing = "Основная",
        hardwareLevel = "FULL",
        sensorOrientation = 90,
        flashAvailable = true,
        aeFpsRanges = ranges.map { "${it.lower}–${it.upper} FPS" },
        fpsRanges = ranges,
        recordingSizes = sizes,
        highSpeedModes = emptyList(),
        videoStabilization = true,
        opticalStabilization = false,
        hdrProfiles = listOf("SDR"),
        detailsMayBeLimited = false
    )
}
