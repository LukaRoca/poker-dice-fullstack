package poker

enum class HandRank(val strength: Int) {
    FIVE_OF_A_KIND(7),
    FOUR_OF_A_KIND(6),
    FULL_HOUSE(5),
    STRAIGHT(4),
    THREE_OF_A_KIND(3),
    TWO_PAIR(2),
    ONE_PAIR(1),
    BUST(0),
}

class Hand(val faces: List<Face>) : Comparable<Hand> {
    init {
        require(faces.size == 5) { "Uma mão é composta por 5 faces" }
    }

    fun rank(): HandRank =
        when {
            isFiveOfAKind() -> HandRank.FIVE_OF_A_KIND
            isFourOfAKind() -> HandRank.FOUR_OF_A_KIND
            isFullHouse() -> HandRank.FULL_HOUSE
            isStraight() -> HandRank.STRAIGHT
            isThreeOfAKind() -> HandRank.THREE_OF_A_KIND
            isTwoPair() -> HandRank.TWO_PAIR
            isOnePair() -> HandRank.ONE_PAIR
            else -> HandRank.BUST
        }

    private fun getRankedValues(): List<Int> {
        val counts = faces.groupingBy { it.rank }.eachCount()
        return counts.entries
            .sortedWith(
                compareByDescending<Map.Entry<Int, Int>> { it.value }
                    .thenByDescending { it.key },
            )
            .flatMap { (rank, count) -> List(count) { rank } }
    }

    override fun compareTo(other: Hand): Int {
        val rankCompare = this.rank().strength.compareTo(other.rank().strength)
        if (rankCompare != 0) {
            return rankCompare
        }

        val thisValues = this.getRankedValues()
        val otherValues = other.getRankedValues()

        for (i in thisValues.indices) {
            val valueCompare = thisValues[i].compareTo(otherValues[i])
            if (valueCompare != 0) {
                return valueCompare
            }
        }

        return 0
    }

    private fun isFiveOfAKind(): Boolean {
        val value = faces.first()
        return faces.all { it == value }
    }

    private fun isFourOfAKind(): Boolean {
        val counts = faces.groupingBy { it }.eachCount()
        return counts.containsValue(4)
    }

    private fun isFullHouse(): Boolean {
        val counts = faces.groupingBy { it }.eachCount().values.sorted()
        return counts == listOf(2, 3)
    }

    private fun isStraight(): Boolean {
        val distinctRanks: Set<Int> = faces.map { it.rank }.toSet()
        if (distinctRanks.size != 5) return false

        val min = distinctRanks.minOrNull()!!
        val max = distinctRanks.maxOrNull()!!

        return max - min == 4
    }

    private fun isThreeOfAKind(): Boolean {
        val counts = faces.groupingBy { it }.eachCount()
        return counts.containsValue(3) && counts.size == 3
    }

    private fun isTwoPair(): Boolean {
        val counts = faces.groupingBy { it }.eachCount().values
        return counts.count { it == 2 } == 2
    }

    private fun isOnePair(): Boolean {
        val counts = faces.groupingBy { it }.eachCount().values
        return counts.count { it == 2 } == 1 && counts.count { it == 1 } == 3
    }
}
