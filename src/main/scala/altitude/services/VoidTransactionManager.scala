package altitude.services

import altitude.dao.TransactionId

class VoidTransactionManager extends AbstractTransactionManager {
  def withTransaction[A](f: => A)(implicit txId: TransactionId) = f
  def asReadOnly[A](f: => A)(implicit txId: TransactionId) = f
}
