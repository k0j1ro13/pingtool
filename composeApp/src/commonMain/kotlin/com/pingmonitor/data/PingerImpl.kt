package com.pingmonitor.data

/**
 * Implementación de [PingerRepository] dependiente de plataforma.
 * Cada target (Android, Desktop, iOS) tiene su propia implementación actual.
 */
expect class PingerImpl() : PingerRepository
