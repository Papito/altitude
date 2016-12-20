package altitude.transactions

import altitude.{Altitude, Context}

class VoidTransactionManager(val app: Altitude) extends AbstractTransactionManager {
  override def withTransaction[A](f: => A)(implicit ctx: Context) = f
  override def asReadOnly[A](f: => A)(implicit ctx: Context) = f
}
