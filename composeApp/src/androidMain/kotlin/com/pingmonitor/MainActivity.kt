package com.pingmonitor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.MobileAds
import com.pingmonitor.data.FavoritesImpl
import com.pingmonitor.data.NetworkInfoImpl
import com.pingmonitor.data.NotifierImpl
import com.pingmonitor.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Contexto para notificaciones e info de red (patrón companion object)
        NotifierImpl.appContext = applicationContext
        NetworkInfoImpl.appContext = applicationContext
        FavoritesImpl.appContext = applicationContext

        // Inicializar AdMob
        MobileAds.initialize(this)

        // Solicitar permiso de notificaciones en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    0
                )
            }
        }

        startKoin {
            androidContext(applicationContext)
            modules(appModule)
        }

        setContent {
            App()
        }
    }
}
