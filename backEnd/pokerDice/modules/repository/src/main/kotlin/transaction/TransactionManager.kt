package transaction

interface TransactionManager {
    fun <R> run(block: (Transaction) -> R): R
}
