package altitude.dao

class MongoTransaction extends Transaction {
  override def close() = Unit
  override def commit() = Unit
  override def rollback() = Unit
  override def setReadOnly(flag: Boolean) = Unit
  override def setAutoCommit(flag: Boolean) = Unit
}