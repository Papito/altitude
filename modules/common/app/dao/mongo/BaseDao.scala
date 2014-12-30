package dao.mongo

import constants.{const => C}
import models.BaseModel
import play.api.libs.json.{JsValue, Json}
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api._
import reactivemongo.core.commands.LastError
import scala.concurrent.ExecutionContext.Implicits.global
import util.log

import scala.concurrent.Future

object BaseDao {
  log.info("Initializing mongo connection", C.tag.DB, C.tag.APP)
  private val driver = new MongoDriver
  private val connection = driver.connection(List("localhost"))
  def db: DB = connection("altitude")
}

abstract class BaseDao[T <: BaseModel](private val collectionName: String) {
  val collection = BaseDao.db.collection[JSONCollection](collectionName)

  def add(model: T): Future[LastError] = {
    collection.insert(Json.toJson(Map("1" -> 1)))
  }
}