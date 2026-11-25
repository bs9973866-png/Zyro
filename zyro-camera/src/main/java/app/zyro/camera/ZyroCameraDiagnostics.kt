package app.zyro.camera

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.DynamicRangeProfiles
import android.media.MediaRecorder
import android.os.Build
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ZyroVideoSize(
    val width: Int,
    val height: Int
) {
    val megapixels: Double
        get() = width.toLong() * height.toLong() / 1_000_000.0
}

data class ZyroCameraInfo(
    val cameraId: String,
    val lensFacing: String,
    val hardwareLevel: String,
    val sensorOrientation: Int?,
    val flashAvailable: Boolean?,
    val aeFpsRanges: List<String>,
    val recordingSizes: List<ZyroVideoSize>,
    val highSpeedModes: List<String>,
    val videoStabilization: Boolean?,
    val opticalStabilization: Boolean?,
    val hdrProfiles: List<String>,
    val detailsMayBeLimited: Boolean
)

data class ZyroCameraReport(
    val cameras: List<ZyroCameraInfo>,
    val warnings: List<String>
)

/**
 * Reads Camera2 characteristics only. It never opens a camera or starts capture.
 * Some characteristics are hidden by Android until CAMERA permission is granted.
 */
object ZyroCameraDiagnostics {

    suspend fun scan(context: Context): ZyroCameraReport = withContext(Dispatchers.IO) {
        scanBlocking(context.applicationContext)
    }

    private fun scanBlocking(context: Context): ZyroCameraReport {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return ZyroCameraReport(emptyList(), listOf("Сервис Camera2 недоступен на этом устройстве."))

        val hasCameraPermission = context.checkSelfPermission(Manifest.permission.CAMERA) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

        val cameraIds = try {
            manager.cameraIdList.toList()
        } catch (error: CameraAccessException) {
            return ZyroCameraReport(emptyList(), listOf("Android не разрешил получить список камер: ${error.message ?: "CameraAccessException"}"))
        } catch (error: SecurityException) {
            return ZyroCameraReport(emptyList(), listOf("Доступ к списку камер ограничен системой."))
        } catch (error: RuntimeException) {
            return ZyroCameraReport(emptyList(), listOf("Не удалось получить список камер: ${error.message ?: "ошибка Android"}"))
        }

        val warnings = mutableListOf<String>()
        val cameras = cameraIds.mapNotNull { cameraId ->
            try {
                val characteristics = manager.getCameraCharacteristics(cameraId)
                val streamMap = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                )

                val recordingSizes = streamMap
                    ?.getOutputSizes(MediaRecorder::class.java)
                    .orEmpty()
                    .map { ZyroVideoSize(it.width, it.height) }
                    .distinctBy { "${it.width}x${it.height}" }
                    .sortedByDescending { it.width.toLong() * it.height.toLong() }

                val aeFpsRanges = characteristics
                    .get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                    .orEmpty()
                    .map { "${it.lower}–${it.upper} FPS" }
                    .distinct()
                    .sorted()

                val highSpeedModes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && streamMap != null) {
                    runCatching {
                        streamMap.highSpeedVideoSizes.flatMap { size ->
                            streamMap.getHighSpeedVideoFpsRangesFor(size).map { range ->
                                "${size.width} × ${size.height}: ${range.lower}–${range.upper} FPS"
                            }
                        }.distinct().sorted()
                    }.getOrElse { emptyList() }
                } else {
                    emptyList()
                }

                val videoStabilizationModes = characteristics
                    .get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
                val opticalStabilizationModes = characteristics
                    .get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)

                val hdrProfiles = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES)
                        ?.supportedProfiles
                        .orEmpty()
                        .map(::dynamicRangeLabel)
                        .distinct()
                        .sorted()
                } else {
                    emptyList()
                }

                val facing = when (characteristics.get(CameraCharacteristics.LENS_FACING)) {
                    CameraCharacteristics.LENS_FACING_FRONT -> "Фронтальная"
                    CameraCharacteristics.LENS_FACING_BACK -> "Основная"
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> "Внешняя"
                    else -> "Не определена"
                }

                val hardwareLevel = when (characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)) {
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL 3"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "EXTERNAL"
                    else -> "Неизвестно"
                }

                ZyroCameraInfo(
                    cameraId = cameraId,
                    lensFacing = facing,
                    hardwareLevel = hardwareLevel,
                    sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION),
                    flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE),
                    aeFpsRanges = aeFpsRanges,
                    recordingSizes = recordingSizes,
                    highSpeedModes = highSpeedModes,
                    videoStabilization = videoStabilizationModes?.contains(
                        CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_ON
                    ),
                    opticalStabilization = opticalStabilizationModes?.contains(
                        CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON
                    ),
                    hdrProfiles = hdrProfiles,
                    detailsMayBeLimited = !hasCameraPermission &&
                        (streamMap == null || aeFpsRanges.isEmpty() || recordingSizes.isEmpty())
                )
            } catch (error: CameraAccessException) {
                warnings += "Камера $cameraId временно недоступна: ${error.message ?: "CameraAccessException"}"
                null
            } catch (error: SecurityException) {
                warnings += "Часть данных камеры $cameraId скрыта разрешениями Android."
                null
            } catch (error: RuntimeException) {
                warnings += "Не удалось прочитать характеристики камеры $cameraId."
                null
            }
        }

        if (cameraIds.isEmpty()) {
            warnings += "Android не обнаружил доступных камер."
        }
        if (!hasCameraPermission) {
            warnings += "Без разрешения CAMERA Android может скрывать часть характеристик. Само сканирование не включает камеру."
        }

        return ZyroCameraReport(cameras, warnings.distinct())
    }

    private fun dynamicRangeLabel(profile: Long): String = when (profile) {
        DynamicRangeProfiles.STANDARD -> "SDR"
        DynamicRangeProfiles.HLG10 -> "HLG10"
        DynamicRangeProfiles.HDR10 -> "HDR10"
        DynamicRangeProfiles.HDR10_PLUS -> "HDR10+"
        else -> "Профиль $profile"
    }
}
