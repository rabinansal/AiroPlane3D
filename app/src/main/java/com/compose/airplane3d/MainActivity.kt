package com.compose.airplane3d

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.animation.LinearInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.compose.airplane3d.AnimationConstants.CAMERA_BASE_ALTITUDE
import com.compose.airplane3d.AnimationConstants.CAMERA_MAX_ALTITUDE
import com.mapbox.geojson.Point
import com.mapbox.geojson.Point.fromLngLat
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.expressions.dsl.generated.match
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.dsl.generated.product
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.modelLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.layers.properties.generated.ModelType
import com.mapbox.maps.extension.style.model.model
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.extension.style.style

/**
 * Main activity hosting the 3D airplane animation using Mapbox.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MapboxAnimationScreen()
        }
    }
}

/**
 * Composable function that creates the Mapbox map view with 3D airplane animation.
 */
@Composable
fun MapboxAnimationScreen() {
    val context = LocalContext.current
    val flightRoute = remember { FlightRoute(context, "flightpath.json") }
    
    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setupMap(flightRoute)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Configure the MapView with style, layers, and animation.
 */
private fun MapView.setupMap(flightRoute: FlightRoute) {
    // Load flight path GeoJSON
    val flightPathJson = try {
        context.assets.open("flightpath.json").bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        ""
    }

    // Create initial airplane feature at start position
    val startPos = flightRoute.sample(0.0)
    val startPoint = fromLngLat(
        startPos?.position?.get(0) ?: -122.384,
        startPos?.position?.get(1) ?: 37.618,
        startPos?.altitude ?: 0.0
    )
    val initialFeature = FeatureBuilder.createInitialFeature(startPoint)

    // Load map style with layers and sources
    // Note: Android SDK doesn't support config() API like JS
    // Using default Standard style which has good lighting
    mapboxMap.loadStyle(
        style(Style.STANDARD) {
            
            // 3D airplane model
            +model("plane") {
                uri("asset://airplane.glb")
            }
            
            // Flight path line
            +geoJsonSource("flightpath") {
                data(flightPathJson)
            }
            +lineLayer("flight-path-line", "flightpath") {
                lineColor("#007cbf")
                lineWidth(8.0)
                lineCap(LineCap.ROUND)
                lineJoin(LineJoin.ROUND)
                lineEmissiveStrength(1.0)
            }

            // Airplane source and model layer
            +geoJsonSource("airplane-source") {
                feature(initialFeature)
            }
            +modelLayer("3d-model-layer", "airplane-source") {
                modelId(get("model-id"))
                modelType(ModelType.LOCATION_INDICATOR)

                // Dynamic scale based on zoom level
                modelScale(
                    interpolate {
                        exponential { literal(0.5) }
                        zoom()
                        literal(2.0)
                        literal(listOf(40000.0, 40000.0, 40000.0))
                        literal(14.0)
                        literal(listOf(1.0, 1.0, 1.0))
                    }
                )

                // Part-specific rotations (gears, propellers) with fallback to main rotation
                modelRotation(
                    match {
                        get("part")
                        literal("front_gear"); get("front-gear-rotation")
                        literal("rear_gears"); get("rear-gear-rotation")
                        literal("propeller_left_outer"); get("propeller-rotation")
                        literal("propeller_left_inner"); get("propeller-rotation")
                        literal("propeller_right_outer"); get("propeller-rotation")
                        literal("propeller_right_inner"); get("propeller-rotation")
                        literal("propeller_left_outer_blur"); get("propeller-rotation-blur")
                        literal("propeller_left_inner_blur"); get("propeller-rotation-blur")
                        literal("propeller_right_outer_blur"); get("propeller-rotation-blur")
                        literal("propeller_right_inner_blur"); get("propeller-rotation-blur")
                        get("rotation")
                    }
                )

                // Light emissions
                modelEmissiveStrength(
                    match {
                        get("part")
                        literal("lights_position_white"); get("light-emission-strobe")
                        literal("lights_position_white_volume"); get("light-emission-strobe")
                        literal("lights_anti_collision_red"); get("light-emission-strobe")
                        literal("lights_anti_collision_red_volume"); get("light-emission-strobe")
                        literal("lights_position_red"); get("light-emission")
                        literal("lights_position_red_volume"); get("light-emission")
                        literal("lights_position_green"); get("light-emission")
                        literal("lights_position_green_volume"); get("light-emission")
                        literal("lights_taxi_white"); get("light-emission-taxi")
                        literal("lights_taxi_white_volume"); get("light-emission-taxi")
                        literal(0.0)
                    }
                )

                // Material opacity for lights and propeller blur
                modelOpacity(
                    match {
                        get("part")
                        literal("lights_position_white_volume"); product { get("light-emission-strobe"); literal(0.25) }
                        literal("lights_anti_collision_red_volume"); product { get("light-emission-strobe"); literal(0.45) }
                        literal("lights_position_green_volume"); product { get("light-emission"); literal(0.25) }
                        literal("lights_position_red_volume"); product { get("light-emission"); literal(0.25) }
                        literal("lights_taxi_white"); product { get("light-emission-taxi"); literal(0.25) }
                        literal("lights_taxi_white_volume"); product { get("light-emission-taxi"); literal(0.25) }
                        literal("propeller_blur"); literal(0.2)
                        literal(1.0)
                    }
                )

                // Correct model translation using pre-calculated property
                modelTranslation(get("model-translation"))
                modelOpacity(1.0)
            }
        }
    ) { style ->
        // Configure Standard style with dusk lighting
        style.setStyleImportConfigProperty("basemap", "lightPreset", com.mapbox.bindgen.Value.valueOf("day"))
        style.setStyleImportConfigProperty("basemap", "showPointOfInterestLabels", com.mapbox.bindgen.Value.valueOf(false))
        style.setStyleImportConfigProperty("basemap", "showRoadLabels", com.mapbox.bindgen.Value.valueOf(false))
        
        // Start animation loop once style is loaded and configured
        startAnimation(this, flightRoute)
    }
}

/**
 * Animation constants matching the JavaScript example.
 */
private object AnimationConstants {
    const val DURATION_MS = 60000.0
    const val ALTITUDE_MIN = 200.0
    const val ALTITUDE_MAX = 3000.0
    const val TIMELAPSE_MIN = 0.001
    const val TIMELAPSE_MAX = 10.0
    const val CAMERA_BASE_ALTITUDE = 50.0
    const val CAMERA_MAX_ALTITUDE = 10000000.0
}

/**
 * Start the animation loop for the airplane.
 */
private fun startAnimation(mapView: MapView, flightRoute: FlightRoute) {
    val mapboxMap = mapView.mapboxMap

    // Initialize airplane state at start position
    val airplane = AirplaneState()
    flightRoute.sample(0.0)?.let { startSample ->
        airplane.position[0] = startSample.position[0]
        airplane.position[1] = startSample.position[1]
        airplane.altitude = startSample.altitude
        airplane.bearing = startSample.bearing
        airplane.pitch = startSample.pitch
    }
    
    var phase = 0.0
    var routeElevation = 0.0
    var lastFrameTime = 0L  // Initialize to 0 to detect first frame

    // Create continuous animation ticker
    val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
    }

    animator.addUpdateListener {
        val currentTime = System.currentTimeMillis()
        
        // Calculate delta time, skip on first frame
        val deltaTime = if (lastFrameTime == 0L) {
            lastFrameTime = currentTime
            16L  // Use standard 60fps frame time for first frame
        } else {
            val delta = (currentTime - lastFrameTime).coerceIn(1, 100)
            lastFrameTime = currentTime
            delta
        }

        // Calculate animation speed based on altitude (timelapse effect)
        val animFade = clamp(
            (routeElevation - AnimationConstants.ALTITUDE_MIN) / 
            (AnimationConstants.ALTITUDE_MAX - AnimationConstants.ALTITUDE_MIN)
        )
        val timelapseFactor = mix(
            AnimationConstants.TIMELAPSE_MIN, 
            AnimationConstants.TIMELAPSE_MAX, 
            animFade * animFade
        )
        
        // Update animation phase
        phase += (deltaTime * timelapseFactor) / AnimationConstants.DURATION_MS
        
        // Stop animation after one complete cycle
        if (phase >= 1.0) {
            animator.cancel()
            phase = 1.0  // Clamp to end
        }

        // Sample route and update airplane state
        flightRoute.sample(flightRoute.totalLength * phase)?.let { target ->
            routeElevation = target.altitude
            airplane.update(target, deltaTime)
        }

        // Update camera to follow airplane (use routeElevation for offset, not airplane.altitude)
        updateCamera(mapboxMap, airplane, routeElevation, animFade)
    }

    // Get the source once to avoid getStyle() overhead in the loop
    mapboxMap.getStyle { style ->
        val source = style.getSource("airplane-source") as? GeoJsonSource
        if (source != null) {
            animator.addUpdateListener {
                val feature = FeatureBuilder.createAnimatedFeature(airplane)
                source.feature(feature)
            }
        }
    }
    
    animator.start()
}



/**
 * Update camera to follow the airplane with dramatic zoom effects.
 * Camera offset creates cinematic effect during takeoff/landing.
 * Uses routeElevation (target altitude) for offset to keep camera close during takeoff/landing.
 */
private fun updateCamera(
    mapboxMap: com.mapbox.maps.MapboxMap,
    airplane: AirplaneState,
    routeElevation: Double,
    animFade: Double
) {
    val point = fromLngLat(airplane.position[0], airplane.position[1], airplane.altitude)
    
    try {
        val camera = mapboxMap.getFreeCameraOptions()
        
        // Camera offset for cinematic effect (matching JS logic exactly)
        // IMPORTANT: Use routeElevation (not airplane.altitude) for offset calculation
        val cameraOffsetLng = mix(-0.0014, 0.0, routeElevation / 200.0)
        val cameraOffsetLat = mix(0.0014, 0.0, routeElevation / 200.0)

        // Camera altitude exactly matching JS: airplane.altitude + 50.0 + mix(0, 10000000, animFade)
        val cameraAltitude = airplane.altitude + CAMERA_BASE_ALTITUDE + mix(0.0, CAMERA_MAX_ALTITUDE, animFade)

        // Position camera with offset
        camera.setLocation(
            fromLngLat(
                airplane.position[0] + cameraOffsetLng,
                airplane.position[1] + cameraOffsetLat
            ),
            cameraAltitude
        )
        
        // Look at the airplane position
        // Note: Android SDK's lookAtPoint doesn't support up vector like JS
        camera.lookAtPoint(point, airplane.altitude)
        
        mapboxMap.setCamera(camera)
    } catch (e: Exception) {
        // Fallback to standard camera with dynamic pitch
        // Higher pitch (more tilted) at low altitude for better view
        val targetZoom = mix(19.0, 8.0, animFade)
        val targetPitch = mix(70.0, 45.0, animFade)  // Steeper angle during takeoff/landing
        
        mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(point)
                .zoom(targetZoom)
                .bearing(0.0)
                .pitch(targetPitch)
                .build()
        )
    }
}