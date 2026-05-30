package poker.token

interface TokenEncoder {
    fun createValidationInformation(token: String): TokenValidationInfo
}
