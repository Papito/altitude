package dao.mongo

import altitude.{Const => C}
import models.BaseModel
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.core.commands.LastError
import util.log
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

import reactivemongo.api._

object BaseDao {
  log.info("Initializing mongo connection", C.tag.DB, C.tag.APP)
  private val driver = new MongoDriver
  private val connection = driver.connection(List("localhost"))
  def db: DB = connection("altitude")
}

abstract class BaseDao[T <: BaseModel](private val collectionName: String) {
  protected def collection = BaseDao.db.collection[JSONCollection](collectionName)

  def add(model: T): Future[LastError] = {
    collection.insert(model.toJson)
  }
}