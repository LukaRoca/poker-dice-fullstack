package poker.invite

import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.util.Base64

@Component
class Sha256InviteHasher : InviteHasher {
    override fun hash(inviteCode: String): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val digest = messageDigest.digest(inviteCode.toByteArray(Charsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
}
