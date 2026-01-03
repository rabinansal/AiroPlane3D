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
     * Optimized for 30fps with reduced interpolation factors.
     * @param target Target position/orientation from flight route
     * @param dtimeMs Delta time in milliseconds since last update
     */
    fun update(target: RouteSample, dtimeMs: Double) {
        val dtimeS = dtimeMs / 1000.0
        animTimeS += dtimeS

        // Smooth interpolation tuned for 30fps (33.33ms per frame)
        // Reduced factors for smoother motion: position = 0.02, rotation = 0.005
        // At 30fps (33ms): position factor = 0.66, rotation factor = 0.165
        val positionFactor = (dtimeMs * 0.02).coerceAtMost(1.0)
        val rotationFactor = (dtimeMs * 0.005).coerceAtMost(1.0)
        
        position[0] = mix(position[0], target.position[0], positionFactor)
        position[1] = mix(position[1], target.position[1], positionFactor)
        altitude = mix(altitude, target.altitude, positionFactor)
        bearing = mix(bearing, target.bearing, rotationFactor)
        pitch = mix(pitch, target.pitch, rotationFactor)
        
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
