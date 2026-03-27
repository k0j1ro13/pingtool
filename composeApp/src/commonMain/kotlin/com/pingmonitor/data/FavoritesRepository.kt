package com.pingmonitor.data

import com.pingmonitor.domain.FavoriteHost

interface FavoritesRepository {
    suspend fun load(): List<FavoriteHost>
    suspend fun save(host: FavoriteHost)
    suspend fun remove(ip: String)
}
