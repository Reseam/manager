package app.reseam.manager

object ManagerAndroidHost {
    init {
        System.loadLibrary("reseam_manager_ffi")
    }

    @JvmStatic
    external fun setClassLoader(classLoader: ClassLoader)
}
