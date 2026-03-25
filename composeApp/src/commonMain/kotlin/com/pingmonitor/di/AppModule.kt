package com.pingmonitor.di

import com.pingmonitor.data.NetworkInfoImpl
import com.pingmonitor.data.NetworkInfoRepository
import com.pingmonitor.data.NetworkScannerImpl
import com.pingmonitor.data.NetworkScannerRepository
import com.pingmonitor.data.PingerImpl
import com.pingmonitor.data.PingerRepository
import com.pingmonitor.domain.NetworkScanUseCase
import com.pingmonitor.domain.PingUseCase
import com.pingmonitor.viewmodel.NetworkInfoViewModel
import com.pingmonitor.viewmodel.NetworkScanViewModel
import com.pingmonitor.viewmodel.PingViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Ping
    single<PingerRepository> { PingerImpl() }
    factory { PingUseCase(get()) }
    viewModel { PingViewModel(get()) }

    // Escáner de red
    single<NetworkScannerRepository> { NetworkScannerImpl() }
    factory { NetworkScanUseCase(get()) }
    viewModel { NetworkScanViewModel(get()) }

    // Información de red
    single<NetworkInfoRepository> { NetworkInfoImpl() }
    viewModel { NetworkInfoViewModel(get()) }
}
