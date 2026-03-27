package com.pingmonitor.data

import com.pingmonitor.domain.FavoriteHost

actual class FavoritesImpl actual constructor() : FavoritesRepository {
    override suspend fun load(): List<FavoriteHost> = emptyList()
    override suspend fun save(host: FavoriteHost) {}
    override suspend fun remove(ip: String) {}
}
