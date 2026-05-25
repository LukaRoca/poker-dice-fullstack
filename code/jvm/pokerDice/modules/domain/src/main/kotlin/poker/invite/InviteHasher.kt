package poker.invite

interface InviteHasher {
    fun hash(inviteCode: String): String
}
