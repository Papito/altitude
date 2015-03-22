package altitude.services

import altitude.dao.Transaction

class VoidTransactionManager extends AbstractTransactionManager {
  def withTransaction[A](f: => A)(implicit tx: Option[Transaction]) = f

  def asReadOnly[A](f: => A)(implicit txArg: Option[Transaction]) = f
}
