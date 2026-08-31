package com.momo.furawalk.platform.android.location

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import com.momo.furawalk.core.domain.provider.HeadingProvider
import com.momo.furawalk.core.domain.provider.LocationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AndroidHeadingProvider(
    private val context: Context,
    private val locationProvider: LocationProvider
) : HeadingProvider, SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val rotationVectorSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _heading = MutableStateFlow(0f)
    override val heading: StateFlow<Float> = _heading.asStateFlow()

    private val _isLowAccuracy = MutableStateFlow(false)
    val isLowAccuracy: StateFlow<Boolean> = _isLowAccuracy.asStateFlow()

    private var declination = 0f
    private var scope: CoroutineScope? = null

    private val smoothingFactor = 0.15f
    private var filteredHeading = 0f
    private var isFirstReading = true

    private val rotationMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    private var lastAccelerometer = FloatArray(3)
    private var lastMagnetometer = FloatArray(3)
    private var isAccelerometerSet = false
    private var isMagnetometerSet = false

    override fun startListening() {
        // スコープのライフサイクルを管理 (stopListeningで破棄できるように設定)
        scope = CoroutineScope(Dispatchers.Main + Job()).apply {
            launch {
                locationProvider.location.collect { location ->
                    location?.let {
                        val field = GeomagneticField(
                            it.latitude.toFloat(),
                            it.longitude.toFloat(),
                            it.altitude.toFloat(),
                            it.time
                        )
                        declination = field.declination
                    }
                }
            }
        }

        if (rotationVectorSensor != null) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_GAME)
        } else {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun stopListening() {
        sensorManager.unregisterListener(this)
        scope?.cancel()
        scope = null
        isFirstReading = true
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        } else {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.size)
                isAccelerometerSet = true
            } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.size)
                isMagnetometerSet = true
            }

            if (isAccelerometerSet && isMagnetometerSet) {
                SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometer, lastMagnetometer)
            } else {
                return
            }
        }

        // 画面の回転（縦向き・横向き）に応じた座標系の再マッピング
        remapForDisplayRotation(rotationMatrix, remappedMatrix)

        SensorManager.getOrientation(remappedMatrix, orientation)

        val azimuthRadians = orientation[0]
        val rawDegrees = Math.toDegrees(azimuthRadians.toDouble()).toFloat()

        val trueNorthDegrees = rawDegrees + declination
        val azimuthDegrees = normalizeAngle(trueNorthDegrees)

        if (isFirstReading) {
            filteredHeading = azimuthDegrees
            isFirstReading = false
        } else {
            filteredHeading = smoothAngle(
                current = filteredHeading,
                target = azimuthDegrees,
                factor = smoothingFactor
            )
        }

        _heading.value = filteredHeading
    }

    private fun remapForDisplayRotation(inMatrix: FloatArray, outMatrix: FloatArray) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        @Suppress("DEPRECATION")
        val rotation = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            context.display?.rotation ?: Surface.ROTATION_0
        } else {
            windowManager?.defaultDisplay?.rotation ?: Surface.ROTATION_0
        }

        var axisX = SensorManager.AXIS_X
        var axisY = SensorManager.AXIS_Y

        when (rotation) {
            Surface.ROTATION_90 -> {
                axisX = SensorManager.AXIS_Y
                axisY = SensorManager.AXIS_MINUS_X
            }
            Surface.ROTATION_180 -> {
                axisX = SensorManager.AXIS_MINUS_X
                axisY = SensorManager.AXIS_MINUS_Y
            }
            Surface.ROTATION_270 -> {
                axisX = SensorManager.AXIS_MINUS_Y
                axisY = SensorManager.AXIS_X
            }
        }

        SensorManager.remapCoordinateSystem(inMatrix, axisX, axisY, outMatrix)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        val isLow = accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE ||
                accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW
        _isLowAccuracy.value = isLow
    }

    private fun normalizeAngle(angle: Float): Float {
        var result = angle % 360f
        if (result < 0f) {
            result += 360f
        }
        return result
    }

    private fun smoothAngle(
        current: Float,
        target: Float,
        factor: Float
    ): Float {
        var delta = target - current

        if (delta > 180f) {
            delta -= 360f
        } else if (delta < -180f) {
            delta += 360f
        }

        val result = current + delta * factor
        return normalizeAngle(result)
    }
}