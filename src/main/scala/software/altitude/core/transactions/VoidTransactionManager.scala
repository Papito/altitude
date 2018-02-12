package software.altitude.core.transactions

import software.altitude.core.AltitudeCoreApp

class VoidTransactionManager(val app: AltitudeCoreApp) extends AbstractTransactionManager {
  override def withTransaction[A](f: => A)(implicit txId: TransactionId): A = f
  override def asReadOnly[A](f: => A)(implicit txId: TransactionId): A = f
}
