package altitude.services

import altitude.dao.Transaction
import altitude.util.log

class JdbcTransactionManager extends AbstractTransactionManager {

  def withTransaction[A](f: => A)(implicit txArg: Option[Transaction] = None) = {
    log.debug("TRANSACTION START")

    val isNestedTx: Boolean = txArg.isDefined

    val tx = if (txArg.isDefined) txArg else Some(new Transaction)

    if (isNestedTx)
      log.debug("Nested transaction: " + tx.hashCode())
    else
      log.debug("New transaction: " + tx.hashCode())

    try {
      if (!isNestedTx) {
        tx.get.conn.setReadOnly(false)
        tx.get.conn.setAutoCommit(false)
      }
      val res: A = f
      commit()
      log.debug("TRANSACTION END: " + tx.hashCode())
      res
    } finally {
      if (!isNestedTx) tx.get.conn.close()
    }
  }

  def asReadOnly[A](f: => A)(implicit txArg: Option[Transaction]) = {
    log.debug("READONLY TRANSACTION START")

    val isNestedTx: Boolean = txArg.isDefined

    val tx = if (txArg.isDefined) txArg else Some(new Transaction)

    if (isNestedTx)
      log.debug("Nested transaction: " + tx.hashCode())
    else
      log.debug("New transaction: " + tx.hashCode())

    try {
      if (!isNestedTx) {
        tx.get.conn.setReadOnly(true)
      }
      val res: A = f
      log.debug("READONLY TRANSACTION END: " + tx.hashCode())
      res
    } finally {
      if (!isNestedTx) tx.get.conn.close()
    }
  }

  def rollback() = {
    log.info("ROLLBACK")
  }

  def commit() = {
    log.info("COMMIT")
  }
}
