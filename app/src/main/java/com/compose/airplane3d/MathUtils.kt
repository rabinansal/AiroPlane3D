package com.compose.airplane3d

import kotlin.math.PI
import kotlin.math.sin

fun clamp(value: Double): Double = value.coerceIn(0.0, 1.0)

fun mix(a: Double, b: Double, factor: Double): Double {
    val t = clamp(factor)
    return a * (1.0 - t) + b * t
}

/** Convert radians to degrees */
fun rad2deg(angRad: Double): Double = (angRad * 180.0) / PI

fun animSinPhaseFromTime(animTimeS: Double, phaseLen: Double): Double {
    return sin(((animTimeS % phaseLen) / phaseLen) * PI * 2.0) * 0.5 + 0.5
}
