package altitude.services

import altitude.Util.log
import altitude.dao.{JdbcTransaction, TransactionId}
import altitude.{Const => C}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

object JdbcTransactionManager {
  val TRANSACTIONS = scala.collection.mutable.Map[Int, JdbcTransaction]()

  def transaction(implicit txId: TransactionId): JdbcTransaction = {
    if (TRANSACTIONS.contains(txId.id)) {
      return TRANSACTIONS.get(txId.id).get
    }
    val tx: JdbcTransaction = new JdbcTransaction
    TRANSACTIONS += (tx.id -> tx)
    txId.id = tx.id
    tx
  }
}

class JdbcTransactionManager extends AbstractTransactionManager {

  override def withTransaction[A](f: => Future[A])(implicit txId: TransactionId = new TransactionId) = {
    val tx = JdbcTransactionManager.transaction
    tx.setReadOnly(flag=false) //FIXME: can throw
    tx.setAutoCommit(flag=false) //FIXME: can throw

    tx.up()
    val res: Future[A] = f

    res.onSuccess { case result =>
      try {
        tx.down()
        tx.commit()
      } finally {
        if (!tx.isNested) {
          log.debug(s"TRANSACTION END: ${tx.id}", C.tag.DB)
          JdbcTransactionManager.TRANSACTIONS.remove(txId.id)
        }
        tx.close()
      }
    }

    res.onFailure { case ex =>
      try {
        log.debug(s"TRANSACTION EXIT: ${tx.id}, ${ex.getMessage}", C.tag.DB)
        //ExceptionUtils.printRootCauseStackTrace(ex)
        tx.down()
        tx.rollback()
      } finally {
        if (!tx.isNested) {
          JdbcTransactionManager.TRANSACTIONS.remove(txId.id)
        }
        tx.close()
      }
    }

    res
  }

  override def asReadOnly[A](f: => Future[A])(implicit txId: TransactionId = new TransactionId) = {
    val tx = JdbcTransactionManager.transaction

    tx.setReadOnly(flag=true) //FIXME: can throw

    tx.up()
    val res: Future[A] = f

    if (tx.level == 0) {
      f.onSuccess { case result =>
        log.debug(s"TRANSACTION END: ${tx.id}", C.tag.DB)
        tx.down()
        if (!tx.isNested) {
          JdbcTransactionManager.TRANSACTIONS.remove(txId.id)
          tx.close()
        }
      }

      f.onFailure { case ex =>
        log.debug(s"TRANSACTION EXIT: ${tx.id}, ${ex.getMessage}", C.tag.DB)
        //ExceptionUtils.printRootCauseStackTrace(ex)
        tx.down()
        if (!tx.isNested) {
          JdbcTransactionManager.TRANSACTIONS.remove(txId.id)
          tx.close()
        }
      }
    }
    res
  }
}