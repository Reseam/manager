package app.reseam.manager.domain.installer

import app.reseam.manager.patcher.PatchArtifact

interface PatchedAppInstaller {
    suspend fun install(artifact: PatchArtifact)
}
