package app.reseam.manager.platform

import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Process
import app.reseam.manager.data.Failure
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

private const val ShellPackage = "com.android.shell"

/** `UserHandle.PER_USER_RANGE`: a uid divided by it is the user it belongs to. */
private const val PerUserRange = 100_000

/** `PackageManager.INSTALL_REPLACE_EXISTING`. */
private const val InstallReplaceExisting = 0x2

/**
 * Android's package installer called through Shizuku, so its sessions belong to the shell (or root), which installs and
 * uninstalls without asking. Every binder the installer hands back is wrapped too, or calls on it would come from
 * Manager's own uid again.
 */
internal class ShizukuPackageInstaller private constructor(private val service: Any) {
    val installer: PackageInstaller = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PackageInstaller::class.java.getConstructor(InstallerInterface, String::class.java, String::class.java, Int::class.javaPrimitiveType)
            .newInstance(service, ShellPackage, null, Process.myUid() / PerUserRange)
    } else {
        PackageInstaller::class.java.getConstructor(InstallerInterface, String::class.java, Int::class.javaPrimitiveType)
            .newInstance(service, ShellPackage, Process.myUid() / PerUserRange)
    }

    /** Android adds the replace flag to every app's session but the shell's, which asks for it as `pm install -r` does. */
    fun createSession(): Int {
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val flags = params.javaClass.getField("installFlags")
        flags.setInt(params, flags.getInt(params) or InstallReplaceExisting)
        return installer.createSession(params)
    }

    fun openSession(id: Int): PackageInstaller.Session {
        val session = service.javaClass.getMethod("openSession", Int::class.javaPrimitiveType).invoke(service, id)!!
        return PackageInstaller.Session::class.java.getConstructor(SessionInterface).newInstance(session.binder.throughShizuku(SessionInterface))
    }

    companion object {
        private val InstallerInterface: Class<*> by lazy { Class.forName("android.content.pm.IPackageInstaller") }
        private val SessionInterface: Class<*> by lazy { Class.forName("android.content.pm.IPackageInstallerSession") }

        /** Calling into Shizuku without its binder throws, so the binder is checked before the permission. */
        val ready: Boolean get() = Shizuku.pingBinder() && !Shizuku.isPreV11() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

        fun connect(): ShizukuPackageInstaller {
            if (!Shizuku.pingBinder()) throw Failure.ShizukuNotRunning()
            if (!ready) throw Failure.ShizukuNotAllowed()
            HiddenApiBypass.addHiddenApiExemptions("Landroid/content/pm/")
            val packageManager = SystemServiceHelper.getSystemService("package").throughShizuku(Class.forName("android.content.pm.IPackageManager"))
            val installer = packageManager.javaClass.getMethod("getPackageInstaller").invoke(packageManager)!!
            return ShizukuPackageInstaller(installer.binder.throughShizuku(InstallerInterface))
        }

        private val Any.binder: IBinder get() = javaClass.getMethod("asBinder").invoke(this) as IBinder

        /** The [aidl] interface over this binder, with every call made by Shizuku's process. */
        private fun IBinder.throughShizuku(aidl: Class<*>): Any =
            Class.forName("${aidl.name}\$Stub").getMethod("asInterface", IBinder::class.java).invoke(null, ShizukuBinderWrapper(this))!!
    }
}
