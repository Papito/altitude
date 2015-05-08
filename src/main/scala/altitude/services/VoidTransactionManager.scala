package altitude.services

import altitude.dao.TransactionId

import scala.concurrent.Future

class VoidTransactionManager extends AbstractTransactionManager {
  def withTransaction[A](f: => Future[A])(implicit txId: TransactionId) = f
  def asReadOnly[A](f: => Future[A])(implicit txId: TransactionId) = f
}
