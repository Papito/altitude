package altitude.transactions

import altitude.{Altitude, Context}

abstract class AbstractTransactionManager {
  val app: Altitude
  def withTransaction[A](f: => A)(implicit ctx: Context): A
  def asReadOnly[A](f: => A)(implicit ctx: Context): A
}
