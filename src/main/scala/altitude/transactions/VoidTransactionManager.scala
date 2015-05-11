package altitude.transactions

import altitude.Altitude

class VoidTransactionManager(val app: Altitude) extends AbstractTransactionManager {
  def withTransaction[A](f: => A)(implicit txId: TransactionId) = f
  def asReadOnly[A](f: => A)(implicit txId: TransactionId) = f
}
