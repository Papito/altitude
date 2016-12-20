package altitude

import altitude.models.User
import altitude.transactions.TransactionId

class Context(val txId: TransactionId = new TransactionId)
             (implicit val repoId: Option[RepositoryId] = None, val user: Option[User] = None)
