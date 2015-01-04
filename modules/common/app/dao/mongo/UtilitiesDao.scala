package dao.mongo

import reactivemongo.core.commands.DropDatabase
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Await

class UtilitiesDao extends BaseMongoDao[Nothing, Nothing]("") with dao.UtilitiesDao {
  override def dropDatabase(): Unit = {
    val f = BaseMongoDao.db.command[Boolean](new DropDatabase)
    Await.result(f, 1.seconds)
  }
}
