package altitude.services

import altitude.dao.Transaction

abstract class AbstractTransactionManager {
  def withTransaction[A](f: => A)(implicit tx: Option[Transaction]): A
  def asReadOnly[A](f: => A)(implicit txArg: Option[Transaction]): A
  def txInstance: Option[Transaction]
}
