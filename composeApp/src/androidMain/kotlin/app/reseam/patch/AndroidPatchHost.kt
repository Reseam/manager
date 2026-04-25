// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patch

/**
 * Android-only host controls for Kotlin patch loading.
 *
 * Manager applications should install the ClassLoader that can resolve the
 * Reseam SDK and patch classes before invoking bundle inspection or patching.
 */
object AndroidPatchHost {
    init {
        System.loadLibrary("reseam_patcher")
    }

    @JvmStatic
    external fun setClassLoader(classLoader: ClassLoader)

    @JvmStatic
    external fun clearClassLoader()
}
