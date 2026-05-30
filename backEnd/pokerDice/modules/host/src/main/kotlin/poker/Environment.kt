package poker

object Environment {
    fun getDbUrl() = System.getenv(KEY_DB_URL) ?: throw Exception("Missing env var $KEY_DB_URL")

    private const val KEY_DB_URL = "DB_URL"
}

// DB_URL=jdbc:postgresql://localhost:5432/daw
// jdbc:postgresql://localhost:5432/daw?user=postgres&password=tubarao
// DB_URL=jdbc:postgresql://localhost:5433/DAW
