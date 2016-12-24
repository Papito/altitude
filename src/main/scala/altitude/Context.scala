package altitude

import altitude.models.{Repository, User}
import altitude.transactions.TransactionId

class Context(val repo: Repository, val user: User)
