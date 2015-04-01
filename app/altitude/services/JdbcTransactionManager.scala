package altitude.services

import altitude.dao.Transaction
import altitude.util.log

class JdbcTransactionManager extends AbstractTransactionManager {

  def withTransaction[A](f: => A)(implicit txArg: Option[Transaction] = Some(new Transaction)) = {
    log.debug("TRANSACTION START")

    val tx: Transaction = txArg.get

    if (tx.isNested)
      log.debug("Nested transaction: " + tx.id)
    else
      log.debug("New transaction: " + tx.id)

    try {
      if (!tx.isNested) {
        tx.setReadOnly(false)
        tx.setAutoCommit(false)
      }

      tx.level += 1
      val res: A = f
      tx.level -= 1

      // commit if this is not an existing transaction
      if (!tx.isNested) tx.commit()
      log.debug("TRANSACTION END: " + tx.id)

      res
    } finally {
      if (!tx.isNested) {
        tx.close()
      }
    }
  }

  def asReadOnly[A](f: => A)(implicit txArg: Option[Transaction] = Some(new Transaction)) = {
    log.debug("READONLY TRANSACTION START")

    val tx: Transaction = txArg.get

    if (tx.isNested)
      log.debug("Nested transaction: " + tx.id)
    else
      log.debug("New transaction: " + tx.id)

    try {
      if (!tx.isNested){
        tx.setReadOnly(true)
      }

      tx.level += 1
      val res: A = f
      tx.level -= 1

      log.debug("READONLY TRANSACTION END: " + tx.id)

      res
    } finally {
      if (!tx.isNested) {
        tx.close()
      }
    }
  }
}
