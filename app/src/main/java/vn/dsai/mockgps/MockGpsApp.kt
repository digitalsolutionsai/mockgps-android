package vn.dsai.mockgps

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import org.maplibre.android.MapLibre

class MockGpsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(MockService.CHANNEL_ID, getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_LOW),
        )
    }
}
