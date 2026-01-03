package com.compose.airplane3d

import android.content.Context
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import java.io.InputStreamReader
import kotlin.math.atan2
import kotlin.math.max

data class RouteSample(
    val position: List<Double>,
    val altitude: Double,
    val bearing: Double,
    val pitch: Double
)

class FlightRoute(private val context: Context, private val assetsFileName: String) {
    private var distances: List<Double> = emptyList()
    private var coordinates: List<List<Double>> = emptyList()
    private var elevationData: List<Double> = emptyList()
    var totalLength: Double = 0.0
        private set

    init {
        loadData()
    }

    private fun loadData() {
        try {
            val jsonString = context.assets.open(assetsFileName).bufferedReader().use { it.readText() }
            val featureCollection = FeatureCollection.fromJson(jsonString)
            val feature = featureCollection.features()?.firstOrNull() ?: return
            
            // Mapbox GeoJSON parser handles geometry
            val lineString = feature.geometry() as? LineString ?: return
            coordinates = lineString.coordinates().map { point ->
                listOf(point.longitude(), point.latitude())
            }

            // Extract elevation from properties
            // The JSON structure in the example has "properties": { "elevation": [...] }
            val properties = feature.properties()
            if (properties != null && properties.has("elevation")) {
                val elevationJsonArray = properties.get("elevation").asJsonArray
                elevationData = elevationJsonArray.map { it.asDouble }
            }
            
            if (elevationData.size != coordinates.size) {
                // Handle mismatch if necessary or default to 0
            }

            // Calculate distances
            val dists = ArrayList<Double>()
            dists.add(0.0)
            for (i in 1 until coordinates.size) {
                 val p1 = com.mapbox.geojson.Point.fromLngLat(coordinates[i-1][0], coordinates[i-1][1])
                 val p2 = com.mapbox.geojson.Point.fromLngLat(coordinates[i][0], coordinates[i][1])
                 val segmentDist = TurfMeasurement.distance(p1, p2, TurfConstants.UNIT_KILOMETERS) * 1000.0
                 dists.add(dists.last() + segmentDist)
            }
            distances = dists
            totalLength = distances.last()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sample(currentDistance: Double): RouteSample? {
        if (distances.isEmpty()) return null

        // Find segment index
        // Use binary search or simple loop. Since usually incremental, simple search might work or findIndex logic
        // Kotlin binarySearch can work if list is efficient access. 
        // Let's stick to the logic from JS example: findIndex(d => d >= currentDistance) - 1
        
        var segmentIndex = distances.indexOfFirst { it >= currentDistance } - 1
        if (segmentIndex < -1) { // If all are smaller? indexOfFirst returns -1 if not found. 
             // If currentDistance > totalLength, indexOfFirst is -1.
             // If currentDistance is 0, indexOfFirst is 0 (d[0] >= 0), so segmentIndex = -1.
             // We need to handle bounds.
        }
        
        // Correct logic for finding segment:
        // We want largest index i such that distances[i] <= currentDistance.
        // Or JS: findIndex(d => d >= currentDistance) - 1. 
        // If currentDistance is 0, distances[0] >= 0 is index 0. 0 - 1 = -1. So we clamp to 0.
        
        segmentIndex = distances.indexOfFirst { it >= currentDistance }
        if (segmentIndex < 0) {
            // Not found, meaning currentDistance > all distances, or list empty.
             if (currentDistance >= totalLength) segmentIndex = distances.size - 1
             else segmentIndex = 1 // Fallback?
        }
        
        // Adjust to be the start of the segment
        segmentIndex -= 1
        if (segmentIndex < 0) segmentIndex = 0
        if (segmentIndex >= coordinates.size - 1) segmentIndex = coordinates.size - 2

        val p1 = coordinates[segmentIndex]
        val p2 = coordinates[segmentIndex + 1]
        val dist1 = distances[segmentIndex]
        val dist2 = distances[segmentIndex + 1]
        
        val segmentLength = dist2 - dist1
        val segmentRatio = if (segmentLength > 0) (currentDistance - dist1) / segmentLength else 0.0
        
        val e1 = elevationData.getOrElse(segmentIndex) { 0.0 }
        val e2 = elevationData.getOrElse(segmentIndex + 1) { 0.0 }
        
        val pt1 = com.mapbox.geojson.Point.fromLngLat(p1[0], p1[1])
        val pt2 = com.mapbox.geojson.Point.fromLngLat(p2[0], p2[1])
        val bearing = TurfMeasurement.bearing(pt1, pt2)
        
        val altitude = e1 + (e2 - e1) * segmentRatio
        val pitch = Math.toDegrees(atan2(e2 - e1, segmentLength))
        
        val lng = p1[0] + (p2[0] - p1[0]) * segmentRatio
        val lat = p1[1] + (p2[1] - p1[1]) * segmentRatio
        
        return RouteSample(
            position = listOf(lng, lat),
            altitude = altitude,
            bearing = bearing,
            pitch = pitch
        )
    }
}
