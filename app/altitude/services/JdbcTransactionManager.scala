package altitude.services

import altitude.dao.Transaction
import altitude.util.log

class JdbcTransactionManager extends AbstractTransactionManager {

  def withTransaction[A](f: => A)(implicit txArg: Option[Transaction] = Some(new Transaction)) = {
    log.debug("TRANSACTION START")

    val isNestedTx: Boolean = txArg.isDefined

    val tx = if (txArg.isDefined) txArg else Some(new Transaction)

    if (isNestedTx)
      log.debug("Nested transaction: " + tx.get.id)
    else
      log.debug("New transaction: " + tx.get.id)

    try {
      if (!isNestedTx) {
        tx.get.setReadOnly(false)
        tx.get.setAutoCommit(false)
      }
      val res: A = f

      // commit if this is not an existing transaction
      if (!isNestedTx) tx.get.commit()

      log.debug("TRANSACTION END: " + tx.get.id)
      res
    } finally {
      if (!isNestedTx) {
        log.debug("Closing connection for transaction: " + tx.get.id)
        tx.get.close()
      }
    }
  }

  def asReadOnly[A](f: => A)(implicit txArg: Option[Transaction] = Some(new Transaction)) = {
    log.debug("READONLY TRANSACTION START")

    val isNestedTx: Boolean = txArg.isDefined

    val tx = if (txArg.isDefined) txArg else Some(new Transaction)

    if (isNestedTx)
      log.debug("Nested transaction: " + tx.get.id)
    else
      log.debug("New transaction: " + tx.get.id)

    try {
      if (!isNestedTx) tx.get.setReadOnly(true)
      val res: A = f
      log.debug("READONLY TRANSACTION END: " + tx.get.id)
      res
    } finally {
      if (!isNestedTx) {
        log.debug("Closing connection for transaction: " + tx.get.id)
        tx.get.close()
      }
    }
  }
}
