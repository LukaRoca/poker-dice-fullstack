package poker.invite

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

data class Invite(
    val id: Int,
    val codeHash: String,
    val inviterId: Int,
    val createdAt: Instant,
    val usedAt: Instant? = null,
) {
    val isUsed: Boolean
        get() = usedAt != null

    fun isExpired(
        clock: Clock,
        validityDuration: Duration,
    ): Boolean {
        if (isUsed) return true
        return (clock.now() - createdAt) > validityDuration
    }
}
