package altitude

import altitude.models.{Repository, User}
import altitude.transactions.TransactionId

class Context(var user: Option[User] = None,
              var txId: Option[TransactionId] = None,
              var repoId: Option[Repository] = None)
