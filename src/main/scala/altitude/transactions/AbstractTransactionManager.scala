package altitude.transactions

import altitude.Altitude

abstract class AbstractTransactionManager {
  val app: Altitude

  /** Transaction counters.
    * We use these for sanity checks during tests or for runtime stats.
    */
  object transactions {
    var CREATED = 0
    var COMMITTED = 0
    var CLOSED = 0
  }

  def withTransaction[A](f: => A)(implicit txId: TransactionId): A
  def asReadOnly[A](f: => A)(implicit txId: TransactionId): A

  def freeResources(): Unit = Unit
}
