package altitude.transactions

import altitude.Altitude

abstract class AbstractTransactionManager {
  val app: Altitude
  def withTransaction[A](f: => A)(implicit txId: TransactionId): A
  def asReadOnly[A](f: => A)(implicit txId: TransactionId): A
}
