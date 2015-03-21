package altitude.services

import altitude.dao.Transaction

class VoidTransactionManager extends AbstractTransactionManager {
  def withTransaction[A](f: => A)(implicit tx: Option[Transaction]) = f

  def asReadOnly[A](f: => A) = f

  def rollback() = Unit

  def commit() = Unit
}
