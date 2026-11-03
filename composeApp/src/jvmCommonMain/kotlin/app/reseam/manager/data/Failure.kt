package app.reseam.manager.data

import app.reseam.manager.platform.InstallFailure

sealed class Failure(val detail: String? = null) : Exception(detail) {
    class OfficialSignerChanged : Failure()

    class NotSignedByPublisher(val bundle: String) : Failure()

    class NoStableRelease(val publisher: String) : Failure()

    class NotABundle(val url: String) : Failure()

    class NoPatchSourceFound(val url: String) : Failure()

    class LinkUnreachable(val host: String) : Failure()

    class ReleaseUnavailable(val publisher: String) : Failure()

    class InvalidLink(val url: String) : Failure()

    class HumanCheckRequired(val url: String) : Failure()

    class DownloadRefused(message: String) : Failure(message)

    class NoFittingBuild : Failure()

    class WrongDownload(val expected: String) : Failure()

    class NoPackageName : Failure()

    class SourceMissing : Failure()

    class NotInstalled(val packageName: String) : Failure()

    class OriginalSignedDifferently : Failure()

    class OriginalNotInstalled(detail: String) : Failure(detail)

    class MountFailed(val app: String) : Failure()

    class RootCommandFailed(detail: String) : Failure(detail)

    class OutputMissing : Failure()

    class Interrupted : Failure()

    class ConnectionLost(detail: String?) : Failure(detail)

    class InstallFailed(val reason: InstallFailure, detail: String?) : Failure(detail)

    class UpdateUnsupported(val app: String) : Failure()

    class UpdateLosesPatches(val app: String, val patches: List<String>) : Failure()

    class UpdateRenames(val app: String, val packageName: String) : Failure()

    class UpdateNotInstalled(val app: String) : Failure()
}
