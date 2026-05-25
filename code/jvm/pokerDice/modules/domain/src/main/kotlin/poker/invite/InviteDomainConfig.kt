package poker.invite

import kotlin.time.Duration

data class InviteDomainConfig(
    val validityDuration: Duration,
) {
    init {
        require(validityDuration.isPositive())
    }
}
