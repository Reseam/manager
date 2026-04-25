package app.reseam.manager

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform