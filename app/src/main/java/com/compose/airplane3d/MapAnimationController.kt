package com.compose.airplane3d

import android.view.Choreographer
import com.mapbox.geojson.Point.fromLngLat
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSource

/**
 * Controller class that manages the 3D airplane animation.
 * Handles animation loop, airplane state updates, and camera positioning.
 */
class MapAnimationController(
    private val mapView: MapView,
    private val flightRoute: FlightRoute
) {
    
    private val mapboxMap: MapboxMap = mapView.mapboxMap
    private val airplane = AirplaneState()
    
    // Animation state variables (matching JavaScript)
    private var phase = 0.0
    private var routeElevation = 0.0
    private var lastFrameTimeMs = 0.0
    
    /**
     * Animation constants matching the JavaScript example.
     */
    object AnimationConstants {
        const val DURATION_MS = 50000.0  // Matches JS: const animationDuration = 50000
        const val ALTITUDE_MIN = 200.0   // Matches JS: const flightTravelAltitudeMin = 200
        const val ALTITUDE_MAX = 3000.0  // Matches JS: const flightTravelAltitudeMax = 3000
        const val TIMELAPSE_MIN = 0.001  // Matches JS: mix(0.001, 10.0, ...)
        const val TIMELAPSE_MAX = 10.0
        const val CAMERA_BASE_ALTITUDE = 50.0      // Matches JS: airplane.altitude + 50.0
        const val CAMERA_MAX_ALTITUDE = 10000000.0 // Matches JS: mix(0, 10000000.0, animFade)
    }
    
    /**
     * Start the animation loop using Choreographer for frame-perfect sync.
     * Matches JavaScript requestAnimationFrame behavior.
     */
    fun startAnimation() {
        // Initialize airplane at start of the route
        flightRoute.sample(0.0)?.let { startSample ->
            airplane.position[0] = startSample.position[0]
            airplane.position[1] = startSample.position[1]
            airplane.altitude = startSample.altitude
            airplane.bearing = startSample.bearing
            airplane.pitch = startSample.pitch
        }
        
        // Get source reference once
        mapboxMap.getStyle { style ->
            val airplaneSource = style.getSource("airplane-source") as? GeoJsonSource ?: return@getStyle
            
            // Create frame callback (equivalent to requestAnimationFrame)
            val frameCallback = object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    // Convert nanoseconds to milliseconds with high precision
                    val time = frameTimeNanos / 1_000_000.0
                    
                    // Initialize lastFrameTime on first frame
                    if (lastFrameTimeMs == 0.0) {
                        lastFrameTimeMs = time
                        Choreographer.getInstance().postFrameCallback(this)
                        return
                    }
                    
                    // Calculate delta time (exactly like JavaScript)
                    val frameDeltaTime = time - lastFrameTimeMs
                    lastFrameTimeMs = time
                    
                    // Calculate animation fade factor based on altitude
                    val animFade = clamp(
                        (routeElevation - AnimationConstants.ALTITUDE_MIN) /
                                (AnimationConstants.ALTITUDE_MAX - AnimationConstants.ALTITUDE_MIN)
                    )
                    // Time-lapse factor to accelerate animation once aircraft is airborne
                    val timelapseFactor = mix(
                        AnimationConstants.TIMELAPSE_MIN,
                        AnimationConstants.TIMELAPSE_MAX,
                        animFade * animFade
                    )
                    
                    // Update animation phase
                    phase += (frameDeltaTime * timelapseFactor) / AnimationConstants.DURATION_MS
                    
                    lastFrameTimeMs = time
                    
                    // Phase is normalized between 0 and 1
                    // When the animation is finished, reset to loop continuously
                    if (phase > 1.0) {
                        phase = 0.0
                        routeElevation = 0.0
                    }
                    
                    val alongRoute = flightRoute.sample(flightRoute.totalLength * phase)
                    if (alongRoute != null) {
                        routeElevation = alongRoute.altitude
                        
                        // Update airplane state based on sampled route point and frame delta time
                        airplane.update(alongRoute, frameDeltaTime)
                    }
                    
                    // Update 3D model layer based on current airplane state
                    airplaneSource.feature(FeatureBuilder.createAnimatedFeature(airplane))
                    
                    // Update camera position to follow the aircraft
                    updateCamera(mapboxMap, airplane, routeElevation, animFade)
                    
                    // Request next frame (equivalent to window.requestAnimationFrame)
                    Choreographer.getInstance().postFrameCallback(this)
                }
            }
            
            // Start the animation loop
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }
    
    /**
     * Update camera to follow the airplane with dramatic zoom effects.
     * Camera offset creates cinematic effect during takeoff/landing.
     */
    private fun updateCamera(
        mapboxMap: MapboxMap,
        airplane: AirplaneState,
        routeElevation: Double,
        animFade: Double
    ) {
        val point = fromLngLat(airplane.position[0], airplane.position[1], airplane.altitude)
        
        try {
            val camera = mapboxMap.getFreeCameraOptions()
            
            val offsetLng = mix(-0.0014, 0.0, routeElevation / 200.0)
            val offsetLat = mix(0.0014, 0.0, routeElevation / 200.0)
            val altitude = airplane.altitude + AnimationConstants.CAMERA_BASE_ALTITUDE + 
                          mix(0.0, AnimationConstants.CAMERA_MAX_ALTITUDE, animFade)
            
            camera.setLocation(
                fromLngLat(
                    airplane.position[0] + offsetLng,
                    airplane.position[1] + offsetLat
                ),
                altitude
            )
            
            camera.lookAtPoint(
                fromLngLat(
                    airplane.position[0],
                    airplane.position[1],
                    airplane.altitude
                ),
                airplane.altitude
            )
            
            mapboxMap.setCamera(camera)
            
        } catch (e: Exception) {
            // Fallback camera (e.g. for low-end devices)
            val fallbackZoom = mix(19.0, 8.0, animFade)
            val fallbackPitch = mix(70.0, 45.0, animFade)
            
            mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(point)
                    .zoom(fallbackZoom)
                    .bearing(0.0)
                    .pitch(fallbackPitch)
                    .build()
            )
        }
    }
}

