package dao.common.mongo

import reactivemongo.core.commands.DropDatabase

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class UtilitiesDao extends BaseMongoDao[Nothing, Nothing]("") with dao.common.UtilitiesDao {
  override def dropDatabase(): Unit = {
    val f = BaseMongoDao.db.command[Boolean](new DropDatabase)
    Await.result(f, 1.seconds)
  }
}
