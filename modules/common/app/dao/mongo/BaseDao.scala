package dao.mongo

import constants.{const => C}
import reactivemongo.api._
import scala.concurrent.ExecutionContext.Implicits.global
import util.log

object BaseDao {
  log.info("Initializing mongo connection", C.tag.DB, C.tag.APP)
  private val driver = new MongoDriver
  private val connection = driver.connection(List("localhost"))
  def db: DB = connection("altitude")
}

//abstract class BaseDao[T](collectionName: String) extends JsonDao[T, BSONObjectID](BaseDao.db, collectionName)
