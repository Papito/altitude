package software.altitude.core.transactions

import software.altitude.core.Altitude

class VoidTransactionManager(val app: Altitude) extends AbstractTransactionManager {
  override def withTransaction[A](f: => A)(implicit txId: TransactionId) = f
  override def asReadOnly[A](f: => A)(implicit txId: TransactionId) = f
}
