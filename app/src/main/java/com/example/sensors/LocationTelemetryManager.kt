package com.example.sensors

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import com.example.data.model.GpsSignalState
import com.example.data.model.GpsTelemetry
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

class LocationTelemetryManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val _telemetry = MutableStateFlow(GpsTelemetry())
    val telemetry: StateFlow<GpsTelemetry> = _telemetry.asStateFlow()

    private val _horizonTiltDegrees = MutableStateFlow(0f to 0f)
    val horizonTiltDegrees: StateFlow<Pair<Float, Float>> = _horizonTiltDegrees.asStateFlow()

    private val httpClient: okhttp3.OkHttpClient by lazy {
        okhttp3.OkHttpClient.Builder()
            .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    private var isLocationStarted = false
    private var lastGeocodedLocation: Location? = null
    private var lastWeatherLocation: Location? = null
    private var lastWeatherFetchedAtMillis: Long = 0L

    private val gravityReading = FloatArray(3)
    private val magnetReading = FloatArray(3)
    private var hasGravity = false
    private var hasMagnet = false
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            handleNewLocation(location)
        }
    }

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ROTATION_VECTOR -> {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    updateCompass(Math.toDegrees(orientationAngles[0].toDouble()).toFloat())
                    updateTilt(
                        pitchDeg = Math.toDegrees(orientationAngles[1].toDouble()).toFloat(),
                        rollDeg = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
                    )
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(event.values, 0, gravityReading, 0, 3)
                    hasGravity = true
                    val rollDeg = Math.toDegrees(
                        kotlin.math.atan2(
                            -event.values[0].toDouble(),
                            kotlin.math.sqrt((event.values[1] * event.values[1] + event.values[2] * event.values[2]).toDouble())
                        )
                    ).toFloat()
                    val pitchDeg = Math.toDegrees(
                        kotlin.math.atan2(event.values[1].toDouble(), event.values[2].toDouble())
                    ).toFloat()
                    updateTilt(pitchDeg, rollDeg)
                    computeFallbackCompass()
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(event.values, 0, magnetReading, 0, 3)
                    hasMagnet = true
                    val magnitudeUt = kotlin.math.sqrt(
                        (event.values[0] * event.values[0] +
                            event.values[1] * event.values[1] +
                            event.values[2] * event.values[2]).toDouble()
                    ).toFloat()
                    if (abs(magnitudeUt - (_telemetry.value.magneticFieldMicroTesla ?: 0f)) > 3f) {
                        _telemetry.update { it.copy(magneticFieldMicroTesla = magnitudeUt) }
                    }
                    computeFallbackCompass()
                }
                Sensor.TYPE_PRESSURE -> {
                    val hpa = event.values[0]
                    if (abs(hpa - (_telemetry.value.pressureHpa ?: 0f)) > 0.5f) {
                        _telemetry.update { it.copy(pressureHpa = hpa) }
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private fun computeFallbackCompass() {
        if (hasGravity && hasMagnet) {
            if (SensorManager.getRotationMatrix(rotationMatrix, null, gravityReading, magnetReading)) {
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                updateCompass(Math.toDegrees(orientationAngles[0].toDouble()).toFloat())
            }
        }
    }

    private fun updateTilt(pitchDeg: Float, rollDeg: Float) {
        val (curPitch, curRoll) = _horizonTiltDegrees.value
        if (abs(pitchDeg - curPitch) > 0.4f || abs(rollDeg - curRoll) > 0.4f) {
            _horizonTiltDegrees.value = pitchDeg to rollDeg
        }
    }

    private fun updateCompass(rawDegrees: Float) {
        val normalized = ((rawDegrees % 360f) + 360f) % 360f
        val current = _telemetry.value.compassAzimuth
        val diff = abs(normalized - current)
        if (diff > 1.5f) {
            _telemetry.update {
                it.copy(
                    compassAzimuth = normalized,
                    compassCardinal = GpsTelemetry.azimuthToCardinal(normalized)
                )
            }
        }
    }

    fun startCompass() {
        val rotSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotSensor != null) {
            sensorManager.registerListener(sensorListener, rotSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
                sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stopCompass() {
        sensorManager.unregisterListener(sensorListener)
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(hasLocationPermission: Boolean) {
        if (!hasLocationPermission) {
            _telemetry.update { it.copy(signalState = GpsSignalState.PERMISSION_DENIED) }
            return
        }
        if (isLocationStarted) return
        isLocationStarted = true

        _telemetry.update {
            if (!it.hasFix) it.copy(signalState = GpsSignalState.ACQUIRING) else it
        }

        runCatching {
            fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                if (lastLoc != null) handleNewLocation(lastLoc)
            }
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2500L)
                .setMinUpdateIntervalMillis(1200L)
                .build()
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        }
    }

    fun stopLocationUpdates() {
        if (!isLocationStarted) return
        isLocationStarted = false
        fusedClient.removeLocationUpdates(locationCallback)
    }

    private fun handleNewLocation(location: Location) {
        val signalState = when {
            location.hasAccuracy() && location.accuracy <= 25f -> GpsSignalState.LOCKED
            else -> GpsSignalState.COARSE
        }
        val speedKmh = if (location.hasSpeed()) location.speed * 3.6f else null
        val altitude = if (location.hasAltitude()) location.altitude else null
        val accuracy = if (location.hasAccuracy()) location.accuracy else null
        val bearing = if (location.hasBearing()) location.bearing else null

        _telemetry.update {
            it.copy(
                latitude = location.latitude,
                longitude = location.longitude,
                altitudeMeters = altitude,
                accuracyMeters = accuracy,
                speedKmh = speedKmh,
                bearingDegrees = bearing,
                signalState = signalState,
                lastUpdatedMillis = System.currentTimeMillis()
            )
        }

        val prev = lastGeocodedLocation
        if (prev == null || prev.distanceTo(location) > 35f) {
            lastGeocodedLocation = location
            reverseGeocode(location)
        }

        val now = System.currentTimeMillis()
        val prevWeatherLoc = lastWeatherLocation
        val movedFar = prevWeatherLoc == null || prevWeatherLoc.distanceTo(location) > 2000f
        val staleWeather = (now - lastWeatherFetchedAtMillis) > 15 * 60 * 1000L
        if (movedFar || staleWeather) {
            lastWeatherLocation = location
            lastWeatherFetchedAtMillis = now
            fetchWeatherInBackground(location.latitude, location.longitude)
        }
    }

    private fun fetchWeatherInBackground(lat: Double, lon: Double) {
        scope.launch {
            runCatching {
                val url = String.format(
                    Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,wind_speed_10m",
                    lat,
                    lon
                )
                val req = okhttp3.Request.Builder().url(url).build()
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val body = resp.body?.string() ?: return@use
                    val current = org.json.JSONObject(body).optJSONObject("current") ?: return@use
                    val temp = if (current.has("temperature_2m")) current.optDouble("temperature_2m").toFloat() else null
                    val humidity = if (current.has("relative_humidity_2m")) current.optInt("relative_humidity_2m") else null
                    val wind = if (current.has("wind_speed_10m")) current.optDouble("wind_speed_10m").toFloat() else null
                    _telemetry.update {
                        it.copy(
                            weatherTempCelsius = temp,
                            weatherHumidityPercent = humidity,
                            weatherWindKmh = wind
                        )
                    }
                }
            }
        }
    }

    private fun reverseGeocode(location: Location) {
        if (!Geocoder.isPresent()) return
        val geocoder = Geocoder(context, Locale.getDefault())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                    addresses.firstOrNull()?.let { addr ->
                        applyAddress(addr)
                    }
                }
            }
        } else {
            scope.launch {
                runCatching {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    addresses?.firstOrNull()?.let { addr ->
                        applyAddress(addr)
                    }
                }
            }
        }
    }

    private fun applyAddress(addr: android.location.Address) {
        val street = addr.getAddressLine(0) ?: listOfNotNull(
            addr.subThoroughfare,
            addr.thoroughfare,
            addr.subLocality
        ).joinToString(" ")
        val cityState = listOfNotNull(addr.locality, addr.adminArea)
            .distinct()
            .joinToString(", ")
        val country = addr.countryName.orEmpty()
        val countryCode = addr.countryCode.orEmpty()
        val postal = addr.postalCode.orEmpty()

        _telemetry.update {
            it.copy(
                addressLine = street,
                cityState = cityState,
                country = country,
                countryCode = countryCode,
                postalCode = postal
            )
        }
    }
}
