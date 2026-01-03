package com.compose.airplane3d

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.geojson.Point.fromLngLat
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.expressions.dsl.generated.match
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.modelLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.layers.properties.generated.ModelType
import com.mapbox.maps.extension.style.model.model
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
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
                modelType(ModelType.COMMON_3D)

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
        // Configure Standard style with day lighting
        style.setStyleImportConfigProperty("basemap", "lightPreset", com.mapbox.bindgen.Value.valueOf("day"))
        style.setStyleImportConfigProperty("basemap", "showPointOfInterestLabels", com.mapbox.bindgen.Value.valueOf(false))
        style.setStyleImportConfigProperty("basemap", "showRoadLabels", com.mapbox.bindgen.Value.valueOf(false))
        
        // Create animation controller and start animation
        val animationController = MapAnimationController(this, flightRoute)
        animationController.startAnimation()
    }
}
