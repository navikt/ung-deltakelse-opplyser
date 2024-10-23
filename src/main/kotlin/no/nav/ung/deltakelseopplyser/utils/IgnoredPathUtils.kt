package no.nav.ung.deltakelseopplyser.utils

object IgnoredPathUtils {
    val IGNORED_PATHS = listOf("/liveness", "/readiness", "/metrics")
    
    fun isIgnoredPath(path: String): Boolean {
        val lastPath = path.substringAfterLast("/")
        return IGNORED_PATHS.contains("/$lastPath")
    }
}
