package poker.jdbi

import jakarta.inject.Named
import org.jdbi.v3.core.Jdbi
import transaction.Transaction
import transaction.TransactionManager

@Named
class JdbiTransactionManager(
    private val jdbi: Jdbi,
) : TransactionManager {
    override fun <R> run(block: (Transaction) -> R): R =
        jdbi.inTransaction<R, Exception> { handle ->
            val transaction = JdbiTransaction(handle)
            block(transaction)
        }
}
