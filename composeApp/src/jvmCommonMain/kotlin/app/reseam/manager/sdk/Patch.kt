package app.reseam.manager.sdk

import app.reseam.sdk.*

val PatchResult.chosen: Boolean get() = !hidden && requiredBy.isEmpty()
val PatchArtifact.path: String get() = when (this) {
    is PatchArtifact.SingleFile -> path
    is PatchArtifact.SplitDir -> path
}
