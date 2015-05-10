package altitude.services

import altitude.dao.TransactionId

abstract class AbstractTransactionManager {
  def withTransaction[A](f: => A)(implicit txId: TransactionId): A
  def asReadOnly[A](f: => A)(implicit txId: TransactionId): A
}
