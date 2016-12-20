package altitude

import altitude.models.User
import altitude.transactions.TransactionId

class Context(val txId: TransactionId = new TransactionId,
              val repoId: RepositoryId,
              val user: User)
