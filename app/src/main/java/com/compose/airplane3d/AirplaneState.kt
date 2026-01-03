package com.compose.airplane3d

import kotlin.math.PI
import kotlin.math.sin

/**
 * Represents the state of an animated airplane.
 * Handles position, orientation, and visual effects (lights, gears, etc.)
 * with physics-based interpolation for smooth motion.
 */
class AirplaneState {
    // Position and orientation
    var position = doubleArrayOf(0.0, 0.0)
    var altitude = 0.0
    var bearing = 0.0
    var pitch = 0.0
    var roll = 0.0
    
    // Mechanical parts
    var frontGearRotation = 0.0
    var rearGearRotation = 0.0
    
    // Lighting effects
    var lightPhase = 0.0
    var lightPhaseStrobe = 0.0
    var lightTaxiPhase = 0.0
    
    // Animation time tracker
    var animTimeS = 0.0

    /**
     * Update airplane state with smooth interpolation toward target values.
     * @param target Target position/orientation from flight route
     * @param dtimeMs Delta time in milliseconds since last update
     */
    fun update(target: RouteSample, dtimeMs: Long) {
        val dtimeS = dtimeMs / 1000.0
        animTimeS += dtimeS

        // Physics-based interpolation for smooth motion
        // Mix factors match JavaScript implementation
        position[0] = mix(position[0], target.position[0], dtimeMs * 0.05)
        position[1] = mix(position[1], target.position[1], dtimeMs * 0.05)
        altitude = mix(altitude, target.altitude, dtimeMs * 0.05)
        bearing = mix(bearing, target.bearing, dtimeMs * 0.01)
        pitch = mix(pitch, target.pitch, dtimeMs * 0.01)
        
        // Gear animation: Retract at 50m altitude
        frontGearRotation = mix(0.0, 90.0, (altitude / 50.0).coerceIn(0.0, 1.0))
        rearGearRotation = mix(0.0, -90.0, (altitude / 50.0).coerceIn(0.0, 1.0))
        
        // Light animations
        lightPhase = animSinPhaseFromTime(animTimeS, 2.0) * 0.25 + 0.75
        lightPhaseStrobe = animSinPhaseFromTime(animTimeS, 1.0)
        lightTaxiPhase = mix(1.0, 0.0, (altitude / 100.0).coerceIn(0.0, 1.0))
        
        // Roll animation: Banking effect at altitude
        val rollTarget = sin(animTimeS * PI * 0.2) * 0.1
        val rollMix = mix(0.0, rollTarget, ((altitude - 50.0) / 100.0).coerceIn(0.0, 1.0))
        roll = rad2deg(rollMix)
    }
}
