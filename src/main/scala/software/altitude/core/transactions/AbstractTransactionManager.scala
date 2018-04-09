package software.altitude.core.transactions

import software.altitude.core.AltitudeCoreApp

/**
 * The transaction manager, created for each specific type of database, and if supported,
 * ensures that a particular block of service code is atomically committed after
 * it exits successfully, or rolled back on failure (or forced to do so during testing)
 *
 * For example:
 *
 * <code>
 * withTransaction[T] {
 *
 * } // commit after done, if outside of scope of other transactions
 * </code>
 *
 * <code>
 * asReadOnly[T] {
 *
 * } // do not commit, but respect the scope of outside transactions
 * </code>
 *
 * The scope is important: a write transaction that is nested within another write transaction
 * will NOT commit. Only the outer-most withTransaction{} block will commit, and so the caller
 * that initiates the transaction first is the only one that can end it.
 *
 * <code>
 * withTransaction[T] {
 *   withTransaction[T] {
 *   } // will NOT commit
 * } // WILL commit
 * </code>
 */
abstract class AbstractTransactionManager {
  val app: AltitudeCoreApp

  /** Transaction counters.
    * We use these for sanity checks during tests or for runtime stats.
    */
  object transactions {
    var CREATED = 0
    var COMMITTED = 0
    var CLOSED = 0

    def reset(): Unit = {
      CREATED = 0
      COMMITTED = 0
      CLOSED = 0
    }
  }

  def withTransaction[A](f: => A)(implicit txId: TransactionId): A
  def savepoint()(implicit txId: TransactionId): Unit = throw new NotImplementedError
  def asReadOnly[A](f: => A)(implicit txId: TransactionId): A
  def freeResources(): Unit = Unit
}
