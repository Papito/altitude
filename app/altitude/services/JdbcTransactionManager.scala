package altitude.services

import altitude.dao.Transaction
import altitude.util.log
import play.api.db.DB
import play.api.Play.current

class JdbcTransactionManager extends AbstractTransactionManager {
  private val ds = DB.getDataSource("postgres")

  def withTransaction[A](f: => A)(implicit txArg: Option[Transaction] = None) = {
    log.info("TRANSACTION START")

    val isNestedTx: Boolean = txArg.isDefined

    implicit val tx = if (txArg.isDefined) txArg else Some(new Transaction(ds))

    if (isNestedTx)
      log.debug("Nested transaction: " + tx.hashCode())
    else
      log.debug("New transaction: " + tx.hashCode())

    try {
      tx.get.conn.setReadOnly(false)
      tx.get.conn.setAutoCommit(false)
      val res: A = f
      commit()
      log.info("TRANSACTION END: " + tx.hashCode())
      res
    } finally {
      if (isNestedTx) tx.get.conn.close()
    }
  }

  def asReadOnly[A](f: => A) = {
    log.info("READONLY")
    f
  }

  def rollback() = {
    log.info("ROLLBACK")
  }

  def commit() = {
    log.info("COMMIT")
  }
}
