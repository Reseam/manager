package app.reseam.manager.sdk

import app.reseam.sdk.PatchArtifact
import app.reseam.sdk.PatchResult

val PatchResult.chosen: Boolean get() = !hidden && requiredBy.isEmpty()
val PatchArtifact.path: String get() = when (this) {
    is PatchArtifact.SingleFile -> path
    is PatchArtifact.SplitDir -> path
}
