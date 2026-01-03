package com.compose.airplane3d

import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Mathematical helper functions for animation interpolation.
 * These match the JavaScript example's helper functions.
 */

/** Clamp value between 0.0 and 1.0 */
fun clamp(v: Double): Double = max(0.0, min(v, 1.0))

/**
 * Linear interpolation between two values.
 * @param a Start value
 * @param b End value
 * @param mixFactor Interpolation factor (0.0 to 1.0)
 * @return Interpolated value
 */
fun mix(a: Double, b: Double, mixFactor: Double): Double {
    val f = clamp(mixFactor)
    return a * (1 - f) + b * f
}

/** Convert radians to degrees */
fun rad2deg(angRad: Double): Double = (angRad * 180.0) / PI

/**
 * Generate a smooth sine wave phase value for animation.
 * @param animTimeS Current animation time in seconds
 * @param phaseLen Period length of the sine wave
 * @return Value oscillating between 0.0 and 1.0
 */
fun animSinPhaseFromTime(animTimeS: Double, phaseLen: Double): Double {
    return sin(((animTimeS % phaseLen) / phaseLen) * PI * 2.0) * 0.5 + 0.5
}
