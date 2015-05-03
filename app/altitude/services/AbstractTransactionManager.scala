package altitude.services

import altitude.dao.TransactionId

import scala.concurrent.Future

abstract class AbstractTransactionManager {
  def withTransaction[A](f: => Future[A])(implicit txId: TransactionId): Future[A]
  def asReadOnly[A](f: => Future[A])(implicit txId: TransactionId): Future[A]
}
