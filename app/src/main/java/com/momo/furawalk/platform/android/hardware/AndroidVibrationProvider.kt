package com.momo.furawalk.platform.android.hardware

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.momo.furawalk.core.domain.provider.VibrationProvider

class AndroidVibrationProvider(context: Context) : VibrationProvider {
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    override fun vibrateOnce() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 (API 29) 以上向けの効果
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            vibrator.vibrate(100)
        }
    }

    override fun vibrateSuccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 100, 50, 100)
            val amplitudes = intArrayOf(0, 255, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            vibrator.vibrate(longArrayOf(0, 100, 50, 100), -1)
        }
    }

    override fun cancel() {
        vibrator.cancel()
    }
}
