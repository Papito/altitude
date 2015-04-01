package altitude.services

import altitude.dao.{Transaction, JdbcTransaction}
import altitude.util.log

class JdbcTransactionManager extends AbstractTransactionManager {
  override def txInstance: Option[Transaction] = Some(new JdbcTransaction)

  def withTransaction[A](f: => A)(implicit txArg: Option[Transaction] = txInstance) = {
    log.debug("TRANSACTION START")

    val tx: Transaction = txArg.get

    if (tx.isNested)
      log.debug("Nested transaction: " + tx.id)
    else
      log.debug("New transaction: " + tx.id)

    try {
      if (!tx.isNested) {
        tx.setReadOnly(flag=false)
        tx.setAutoCommit(flag=false)
      }

      tx.up()
      val res: A = f
      tx.down()

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

  def asReadOnly[A](f: => A)(implicit txArg: Option[Transaction] = txInstance) = {
    log.debug("READONLY TRANSACTION START")

    val tx: Transaction = txArg.get

    if (tx.isNested)
      log.debug("Nested transaction: " + tx.id)
    else
      log.debug("New transaction: " + tx.id)

    try {
      if (!tx.isNested){
        tx.setReadOnly(flag=true)
      }

      tx.up()
      val res: A = f
      tx.down()

      log.debug("READONLY TRANSACTION END: " + tx.id)

      res
    } finally {
      if (!tx.isNested) {
        tx.close()
      }
    }
  }
}
