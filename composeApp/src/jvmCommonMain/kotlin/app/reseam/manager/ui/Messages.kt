package app.reseam.manager.ui

import androidx.compose.runtime.Composable
import app.reseam.manager.Notice
import app.reseam.manager.data.Failure
import app.reseam.manager.platform.HttpStatusException
import app.reseam.manager.platform.InstallFailure
import app.reseam.manager.resources.*
import app.reseam.sdk.Problem
import app.reseam.sdk.SdkError
import java.io.IOException
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun Throwable.describe(): String = when (this) {
    is Failure -> describe()
    is SdkError -> problem.describe() ?: message
    is HttpStatusException -> stringResource(Res.string.error_http, status)
    is IOException -> if (message.orEmpty().contains("ENOSPC") || message.orEmpty().contains("No space left")) stringResource(Res.string.error_no_space) else stringResource(Res.string.error_file, message.orEmpty())
    else -> message?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.error_unknown)
}

@Composable
private fun Failure.describe(): String = when (this) {
    is Failure.OfficialSignerChanged -> stringResource(Res.string.error_official_signer)
    is Failure.NotSignedByPublisher -> stringResource(Res.string.error_not_signed_by_publisher, bundle)
    is Failure.NoStableRelease -> stringResource(Res.string.error_no_stable_release, publisher)
    is Failure.NotABundle -> stringResource(Res.string.error_not_a_bundle, url)
    is Failure.InvalidLink -> stringResource(Res.string.error_invalid_link, url)
    is Failure.HumanCheckRequired -> stringResource(Res.string.error_human_check)
    is Failure.DownloadRefused -> detail.orEmpty()
    is Failure.NoFittingBuild -> stringResource(Res.string.error_no_fitting_build)
    is Failure.WrongDownload -> stringResource(Res.string.error_wrong_download, expected)
    is Failure.NoPackageName -> stringResource(Res.string.problem_missing_package)
    is Failure.SourceMissing -> stringResource(Res.string.error_source_missing)
    is Failure.KeystoreLocked -> stringResource(Res.string.error_keystore_locked)
    is Failure.KeystoreEmpty -> stringResource(Res.string.error_keystore_empty)
    is Failure.UnsupportedKey -> stringResource(Res.string.error_unsupported_key)
    is Failure.NotInstalled -> stringResource(Res.string.error_not_installed, packageName)
    is Failure.OriginalSignedDifferently -> stringResource(Res.string.error_original_signed_differently)
    is Failure.OriginalNotInstalled -> stringResource(Res.string.error_original_not_installed, detail.orEmpty())
    is Failure.InstalledVersionDiffers -> stringResource(Res.string.error_installed_version_differs, app)
    is Failure.MountFailed -> stringResource(Res.string.error_mount_failed, app)
    is Failure.RootCommandFailed -> stringResource(Res.string.error_root_failed, detail.orEmpty())
    is Failure.OutputMissing -> stringResource(Res.string.error_output_missing)
    is Failure.Interrupted -> stringResource(Res.string.error_interrupted)
    is Failure.ConnectionLost -> stringResource(Res.string.error_connection)
    is Failure.InstallFailed -> installFailed(reason, detail)
}

@Composable
private fun installFailed(reason: InstallFailure, detail: String?): String = when (reason) {
    InstallFailure.Conflict -> stringResource(Res.string.install_conflict)
    InstallFailure.Storage -> stringResource(Res.string.error_no_space)
    InstallFailure.Incompatible -> stringResource(Res.string.install_incompatible)
    InstallFailure.Invalid -> stringResource(Res.string.install_invalid)
    InstallFailure.Blocked -> stringResource(Res.string.install_blocked)
    InstallFailure.Other -> detail?.let { stringResource(Res.string.error_install_failed_detail, it) } ?: stringResource(Res.string.error_install_failed)
}

@Composable
fun Problem.describe(bundleName: String? = null): String? {
    val bundle = bundleName ?: stringResource(Res.string.this_source)
    return when (this) {
        is Problem.BundleTooOld -> stringResource(Res.string.problem_bundle_too_old, bundle)
        is Problem.EngineTooOld -> stringResource(Res.string.problem_engine_too_old, bundle)
        is Problem.UntrustedBundle -> stringResource(Res.string.problem_untrusted, bundle)
        is Problem.UnreadableBundle -> stringResource(Res.string.problem_unreadable_bundle, bundle)
        is Problem.UnreadableApk -> stringResource(Res.string.problem_unreadable_apk)
        is Problem.PatchesFailed -> pluralStringResource(Res.plurals.problem_patches_failed, patches.size, patches.size)
        is Problem.OptionType -> stringResource(Res.string.problem_option_type, key)
        is Problem.OptionChoice -> stringResource(Res.string.problem_option_choice, key, allowed.joinToString())
        is Problem.SingleFileComponents -> stringResource(Res.string.problem_single_file, components.toInt())
        Problem.MissingPackage -> stringResource(Res.string.problem_missing_package)
        is Problem.IncompatiblePackage -> stringResource(Res.string.problem_incompatible_package)
        is Problem.UnknownPreset -> stringResource(Res.string.problem_unknown_preset, value)
        Problem.Other -> null
    }
}

@Composable
fun Notice.text(): String = when (this) {
    Notice.InstallCancelled -> stringResource(Res.string.notice_install_cancelled)
    Notice.InstallUnconfirmed -> stringResource(Res.string.notice_install_unconfirmed)
    Notice.InstallTimedOut -> stringResource(Res.string.notice_install_timed_out)
    Notice.AppMounted -> stringResource(Res.string.notice_mounted)
    Notice.AppUnmounted -> stringResource(Res.string.notice_unmounted)
    Notice.RootRefused -> stringResource(Res.string.notice_root_refused)
    is Notice.AppSaved -> stringResource(Res.string.notice_saved, fileName)
    is Notice.BundleRemoved -> stringResource(Res.string.notice_bundle_removed, name, problem.describe(name).orEmpty()).trim()
    is Notice.SignerToConfirm -> stringResource(Res.string.notice_signer, name)
    is Notice.SyncFailed -> failures.singleOrNull()
        ?.let { stringResource(Res.string.notice_sync_failed_one, it.bundle, it.error.describe()) }
        ?: pluralStringResource(Res.plurals.notice_sync_failed, failures.size, failures.size)
    Notice.KeyImported -> stringResource(Res.string.notice_key_imported)
    is Notice.KeyExported -> stringResource(Res.string.notice_key_exported, fileName)
    Notice.KeyReset -> stringResource(Res.string.notice_key_reset)
    is Notice.Error -> error.describe()
}
