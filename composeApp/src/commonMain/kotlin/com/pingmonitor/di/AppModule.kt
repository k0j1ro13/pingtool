package com.pingmonitor.di

import com.pingmonitor.data.FavoritesImpl
import com.pingmonitor.data.FavoritesRepository
import com.pingmonitor.data.TracerouteImpl
import com.pingmonitor.data.TracerouteRepository
import com.pingmonitor.data.NetworkInfoImpl
import com.pingmonitor.data.NetworkInfoRepository
import com.pingmonitor.data.NetworkScannerImpl
import com.pingmonitor.data.NetworkScannerRepository
import com.pingmonitor.data.NotifierImpl
import com.pingmonitor.data.NotifierRepository
import com.pingmonitor.data.PingerImpl
import com.pingmonitor.data.PingerRepository
import com.pingmonitor.data.SpeedTesterImpl
import com.pingmonitor.data.SpeedTesterRepository
import com.pingmonitor.domain.NetworkScanUseCase
import com.pingmonitor.domain.PingUseCase
import com.pingmonitor.domain.SpeedTestUseCase
import com.pingmonitor.viewmodel.NetworkInfoViewModel
import com.pingmonitor.viewmodel.NetworkScanViewModel
import com.pingmonitor.viewmodel.PingViewModel
import com.pingmonitor.viewmodel.SpeedTestViewModel
import com.pingmonitor.viewmodel.TracerouteViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Favoritos
    single<FavoritesRepository> { FavoritesImpl() }

    // Ping
    single<PingerRepository> { PingerImpl() }
    single<NotifierRepository> { NotifierImpl() }
    factory { PingUseCase(get()) }
    viewModel { PingViewModel(get(), get(), get(), get()) }

    // Escáner de red
    single<NetworkScannerRepository> { NetworkScannerImpl() }
    factory { NetworkScanUseCase(get()) }
    viewModel { NetworkScanViewModel(get()) }

    // Información de red
    single<NetworkInfoRepository> { NetworkInfoImpl() }
    viewModel { NetworkInfoViewModel(get()) }

    // Test de velocidad
    single<SpeedTesterRepository> { SpeedTesterImpl() }
    factory { SpeedTestUseCase(get()) }
    viewModel { SpeedTestViewModel(get()) }

    // Traceroute
    single<TracerouteRepository> { TracerouteImpl() }
    viewModel { TracerouteViewModel(get()) }
}
