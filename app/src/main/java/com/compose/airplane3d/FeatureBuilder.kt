package com.compose.airplane3d

import com.google.gson.JsonArray
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point

/**
 * Helper object for creating GeoJSON features with airplane model properties.
 */
object FeatureBuilder {
    
    /**
     * Create initial feature with default values for all animated properties.
     */
    fun createInitialFeature(startPoint: Point): Feature {
        val zeros = JsonArray().apply { add(0.0); add(0.0); add(0.0) }
        
        return Feature.fromGeometry(startPoint, null, "plane").apply {
            addStringProperty("model-id", "plane")
            addProperty("rotation", zeros)
            addNumberProperty("light-emission", 0.0)
            addNumberProperty("light-emission-strobe", 0.0)
            addNumberProperty("light-emission-taxi", 0.0)
            addProperty("front-gear-rotation", zeros)
            addProperty("rear-gear-rotation", zeros)
            addProperty("propeller-rotation", zeros)
            addProperty("propeller-rotation-blur", zeros)
        }
    }
    
    /**
     * Create animated feature from current airplane state.
     */
    fun createAnimatedFeature(airplane: AirplaneState): Feature {
        val point = Point.fromLngLat(airplane.position[0], airplane.position[1], airplane.altitude)
        
        // Rotation: [roll, pitch, yaw]
        val rotationList = JsonArray().apply {
            add(airplane.roll)
            add(airplane.pitch)
            add(airplane.bearing + 90)
        }
        
        // Gear rotations: [x, y, z]
        val frontGearList = JsonArray().apply {
            add(0.0); add(0.0); add(airplane.frontGearRotation)
        }
        val rearGearList = JsonArray().apply {
            add(0.0); add(0.0); add(airplane.rearGearRotation)
        }
        
        // Propeller animations
        val propAngle = -(airplane.animTimeS % 0.5) * 2.0 * 360.0
        val propList = JsonArray().apply {
            add(0.0); add(0.0); add(propAngle)
        }
        
        val propBlurAngle = (airplane.animTimeS % 0.1) * 10.0 * 360.0
        val propBlurList = JsonArray().apply {
            add(0.0); add(0.0); add(propBlurAngle)
        }
        
        return Feature.fromGeometry(point, null, "plane").apply {
            addStringProperty("model-id", "plane")
            addNumberProperty("z-elevation", airplane.altitude)
            addProperty("model-translation", JsonArray().apply { add(0.0); add(0.0); add(airplane.altitude) })
            addProperty("rotation", rotationList)
            addNumberProperty("light-emission", airplane.lightPhase)
            addNumberProperty("light-emission-strobe", airplane.lightPhaseStrobe)
            addNumberProperty("light-emission-taxi", airplane.lightTaxiPhase)
            addProperty("front-gear-rotation", frontGearList)
            addProperty("rear-gear-rotation", rearGearList)
            addProperty("propeller-rotation", propList)
            addProperty("propeller-rotation-blur", propBlurList)
        }
    }
}
