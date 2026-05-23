/*
 * LFE — A low-feature, on-device AI agent for Android.
 * Copyright (C) 2026 Brad Flaugher
 *
 * Licensed under the GNU General Public License v3.0 or later.
 * See LICENSE in the project root for terms.
 *
 * Includes code adapted from Google AI Edge Gallery (Apache 2.0,
 * Copyright 2025 Google LLC) — https://github.com/google-ai-edge/gallery.
 */
package com.bradflaugher.lfe.customtasks.common

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SteadinessMonitor(context: Context, private val steadyDurationMs: Long = 2000L) :
  SensorEventListener {
  private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
  private val gyroSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

  // If gyroscope is not available, default to stable.
  private val _isStable = MutableStateFlow(gyroSensor == null)
  val isStable: StateFlow<Boolean> = _isStable

  // Threshold: 0.1 rad/s is quite steady.
  private val STABILITY_THRESHOLD = 0.1f

  fun start() {
    gyroSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
  }

  fun stop() {
    sensorManager.unregisterListener(this)
  }

  private var steadyStartTime: Long? = null

  override fun onSensorChanged(event: SensorEvent?) {
    if (event?.sensor?.type == Sensor.TYPE_GYROSCOPE) {
      val x = event.values[0]
      val y = event.values[1]
      val z = event.values[2]

      val magnitude = sqrt(x * x + y * y + z * z)

      if (magnitude < STABILITY_THRESHOLD) {
        if (steadyStartTime == null) {
          steadyStartTime = System.currentTimeMillis()
        }
        val start = steadyStartTime
        if (start != null && System.currentTimeMillis() - start >= steadyDurationMs) {
          _isStable.value = true
        } else {
          _isStable.value = false
        }
      } else {
        steadyStartTime = null
        _isStable.value = false
      }
    }
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
