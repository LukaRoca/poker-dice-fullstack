package poker

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.postgresql.ds.PGSimpleDataSource
import poker.jdbi.configureWithAppRequirements

private val jdbi =
    Jdbi
        .create(
            PGSimpleDataSource().apply {
                setURL("jdbc:postgresql://localhost:5432/daw?user=postgres&password=tubarao&currentSchema=dbo")
            },
        ).configureWithAppRequirements()

fun testWithHandleAndRollback(block: (Handle) -> Unit) =
    jdbi.useTransaction<Exception> { handle ->
        block(handle)
        handle.rollback()
    }
