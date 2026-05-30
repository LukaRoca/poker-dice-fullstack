package poker

object Environment {
    private const val KEY_DB_URL = "DB_URL"
    private const val DEFAULT_DB_URL =
        "jdbc:postgresql://localhost:5432/daw?user=postgres&password=tubarao&currentSchema=dbo"

    fun getDbUrl(): String = System.getenv(KEY_DB_URL) ?: DEFAULT_DB_URL
}
