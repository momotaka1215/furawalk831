package com.momo.furawalk.platform.android.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.momo.furawalk.core.domain.provider.StepProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AndroidStepProvider(context: Context) : StepProvider, SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    private val _currentSteps = MutableStateFlow(0)
    override val currentSteps: StateFlow<Int> = _currentSteps.asStateFlow()

    private var initialSteps: Int = -1
    private var isListening = false

    override fun startListening() {
        if (isListening) return

        // 応答性を上げるため SENSOR_DELAY_GAME を使用し、バッチ遅延を0(即時)に設定
        // 1. リアルタイム歩数検知 (TYPE_STEP_DETECTOR)
        stepDetector?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME, 0)
        }

        // 2. 累積歩数の補正用 (TYPE_STEP_COUNTER)
        stepCounter?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME, 0)
        }

        if (stepCounter == null && stepDetector == null) {
            Log.e("StepProvider", "No step sensors available")
        } else {
            isListening = true
        }
    }

    override fun stopListening() {
        if (!isListening) return
        sensorManager.unregisterListener(this)
        initialSteps = -1
        isListening = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            // 一歩ごとに即座に呼ばれる
            Sensor.TYPE_STEP_DETECTOR -> {
                if (event.values[0] == 1.0f) {
                    _currentSteps.update { it + 1 }
                    Log.d("StepProvider", "Realtime step (Detector): ${_currentSteps.value}")
                }
            }

            // 遅れて届く正確な累積値（Detectorとのズレを自動補正）
            Sensor.TYPE_STEP_COUNTER -> {
                val totalStepsSinceBoot = event.values[0].toInt()

                if (initialSteps == -1 || totalStepsSinceBoot < initialSteps) {
                    initialSteps = totalStepsSinceBoot
                    Log.d("StepProvider", "Initial steps set: $initialSteps")
                }

                val exactSessionSteps = totalStepsSinceBoot - initialSteps

                // DetectorによるカウントとCounterの実測値に差異があればCounterの値で補正
                if (_currentSteps.value != exactSessionSteps) {
                    _currentSteps.value = exactSessionSteps
                    Log.d("StepProvider", "Synced with Counter: $exactSessionSteps")
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}