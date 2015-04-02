package altitude.services

class VoidTransactionManager extends AbstractTransactionManager {
  def withTransaction[A](f: => A)(implicit txId: Int = 0) = f
  def asReadOnly[A](f: => A)(implicit txId: Int = 0) = f
}
