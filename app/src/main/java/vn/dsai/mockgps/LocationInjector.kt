package vn.dsai.mockgps

import android.annotation.SuppressLint
import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.util.Log
import com.google.android.gms.location.LocationServices

/**
 * Feeds one location to every place apps read from: the platform GPS, network and fused
 * providers, plus Google Play services' fused provider, which most apps actually use.
 */
@SuppressLint("MissingPermission")
class LocationInjector(private val ctx: Context) {
    private val lm = ctx.getSystemService(LocationManager::class.java)
    private val gms = LocationServices.getFusedLocationProviderClient(ctx)
    private val added = mutableListOf<String>()
    private var gmsRequested = false

    @Volatile var gmsActive = false
        private set

    fun start(gps: Boolean, network: Boolean, fused: Boolean) {
        val providers = buildList {
            if (gps) add(LocationManager.GPS_PROVIDER)
            if (network) add(LocationManager.NETWORK_PROVIDER)
            if (fused && Build.VERSION.SDK_INT >= 31) add(LocationManager.FUSED_PROVIDER)
        }
        for (name in providers) {
            runCatching { lm.removeTestProvider(name) }
            try {
                addProvider(name)
                lm.setTestProviderEnabled(name, true)
                added += name
            } catch (e: Exception) {
                Log.w(TAG, "Cannot mock provider $name", e)
            }
        }
        if (fused && Permissions.hasLocation(ctx)) {
            gmsRequested = true
            gms.setMockMode(true)
                .addOnSuccessListener { gmsActive = true }
                .addOnFailureListener { Log.w(TAG, "Play services mock mode refused", it) }
        }
    }

    fun push(loc: Location) {
        for (name in added) {
            val copy = Location(loc).apply {
                provider = name
                if (name == LocationManager.NETWORK_PROVIDER) accuracy = maxOf(accuracy, 12f)
            }
            runCatching { lm.setTestProviderLocation(name, copy) }.onFailure { Log.w(TAG, "push $name", it) }
        }
        if (gmsActive) gms.setMockLocation(Location(loc).apply { provider = "fused" })
    }

    fun stop() {
        // setMockMode(true) may still be in flight, so undo whatever was requested.
        if (gmsRequested) runCatching { gms.setMockMode(false) }
        gmsRequested = false
        gmsActive = false
        for (name in added) runCatching { lm.removeTestProvider(name) }
        added.clear()
    }

    private fun addProvider(name: String) {
        val network = name == LocationManager.NETWORK_PROVIDER
        if (Build.VERSION.SDK_INT >= 31) {
            val props = ProviderProperties.Builder()
                .setHasNetworkRequirement(network)
                .setHasSatelliteRequirement(name == LocationManager.GPS_PROVIDER)
                .setHasAltitudeSupport(true)
                .setHasSpeedSupport(true)
                .setHasBearingSupport(true)
                .setPowerUsage(ProviderProperties.POWER_USAGE_LOW)
                .setAccuracy(if (network) ProviderProperties.ACCURACY_COARSE else ProviderProperties.ACCURACY_FINE)
                .build()
            lm.addTestProvider(name, props)
        } else {
            // Criteria and ProviderProperties share the same values; the latter only exists from API 31.
            @Suppress("DEPRECATION")
            @SuppressLint("WrongConstant")
            lm.addTestProvider(
                name, network, name == LocationManager.GPS_PROVIDER, false, false, true, true, true,
                Criteria.POWER_LOW, if (network) Criteria.ACCURACY_COARSE else Criteria.ACCURACY_FINE,
            )
        }
    }

    private companion object {
        const val TAG = "MockGPS"
    }
}
