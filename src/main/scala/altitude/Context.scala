package altitude

import altitude.models.{Repository, User}
import altitude.transactions.TransactionId

class Context(val txId: TransactionId = new TransactionId,
              val repo: Repository,
              val user: User)
