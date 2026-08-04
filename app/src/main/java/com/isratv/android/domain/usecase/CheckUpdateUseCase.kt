package com.isratv.android.domain.usecase

import com.isratv.android.BuildConfig
import com.isratv.android.data.repository.GitHubUpdateRepository
import com.isratv.android.domain.model.UpdateInfo
import javax.inject.Inject

class CheckUpdateUseCase @Inject constructor(
    private val repository: GitHubUpdateRepository
) {
    suspend fun invoke(): UpdateInfo? {
        val release = repository.fetchLatestRelease() ?: return null
        val currentVersion = BuildConfig.VERSION_NAME

        val remoteVersion = release.tagName.removePrefix("v")
        val localVersion = currentVersion.removePrefix("v")

        val hasUpdate = isNewer(remoteVersion, localVersion)

        return UpdateInfo(
            hasUpdate = hasUpdate,
            latestVersion = remoteVersion,
            downloadUrl = release.downloadUrl
        )
    }

    private fun isNewer(remote: String, local: String): Boolean {
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = local.split(".").mapNotNull { it.toIntOrNull() }

        val maxLength = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLength) {
            val remotePart = remoteParts.getOrElse(i) { 0 }
            val localPart = localParts.getOrElse(i) { 0 }

            if (remotePart > localPart) return true
            if (remotePart < localPart) return false
        }
        return false
    }
}