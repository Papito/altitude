package altitude.dao

class MongoTransaction extends Transaction {
  override def close() = Unit
  override def commit() = Unit
  override def rollback() = Unit
}