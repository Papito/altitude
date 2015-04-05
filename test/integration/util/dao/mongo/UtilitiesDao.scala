package integration.util.dao.mongo

import altitude.dao.TransactionId
import altitude.dao.mongo.BaseMongoDao
import reactivemongo.core.commands.DropDatabase

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class UtilitiesDao extends BaseMongoDao("") with integration.util.dao.UtilitiesDao {
  override def dropDatabase(): Unit = {
    val f = BaseMongoDao.db.command[Boolean](new DropDatabase)
    Await.result(f, 2.seconds)
  }

  override def close() = Unit
  override def rollback() = Unit
  override def createTransaction(tx: TransactionId) = Unit
}
