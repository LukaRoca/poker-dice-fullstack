package poker.model

data class DepositInputModel(
    val amount: Int,
) {
    init {
        if (amount < 1) throw IllegalArgumentException("O montante do depósito deve ser positivo")
    }
}
