package com.pingmonitor.data

import android.content.Context
import com.pingmonitor.domain.FavoriteHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class FavoritesImpl actual constructor() : FavoritesRepository {

    companion object {
        var appContext: Context? = null
    }

    private val file: File?
        get() = appContext?.let { File(it.filesDir, "favorites.txt") }

    override suspend fun load(): List<FavoriteHost> = withContext(Dispatchers.IO) {
        file?.takeIf { it.exists() }
            ?.readLines()
            ?.mapNotNull { parseLine(it) }
            ?: emptyList()
    }

    override suspend fun save(host: FavoriteHost) = withContext(Dispatchers.IO) {
        val f = file ?: return@withContext
        val existing = load().toMutableList()
        if (existing.none { it.ip == host.ip }) {
            existing.add(host)
            f.writeText(existing.joinToString("\n") { "${it.ip}\t${it.label}" })
        }
    }

    override suspend fun remove(ip: String) = withContext(Dispatchers.IO) {
        val f = file ?: return@withContext
        val updated = load().filter { it.ip != ip }
        f.writeText(updated.joinToString("\n") { "${it.ip}\t${it.label}" })
    }

    private fun parseLine(line: String): FavoriteHost? {
        val parts = line.split("\t")
        return if (parts.size >= 2) FavoriteHost(parts[0].trim(), parts[1].trim()) else null
    }
}
