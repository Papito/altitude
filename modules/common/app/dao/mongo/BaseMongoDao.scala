package dao.mongo

import altitude.{Const => C}
import dao.BaseDao
import models.BaseModel
import play.api.{Configuration, Play}
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.concurrent.ExecutionContext.Implicits.global
import util.log
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

import reactivemongo.api._

object BaseMongoDao {
  private val host = Play.current.configuration.getString("db.mongo.host").getOrElse("")
  require(host.nonEmpty)
  private val dbName = Play.current.configuration.getString("db.name").getOrElse("")
  require(dbName.nonEmpty)

  log.info(
    "Initializing mongo connection",
    Map("host" -> host, "dbName" -> dbName), C.tag.DB, C.tag.APP)

  final private val driver = new MongoDriver
  final private val connection = driver.connection(List(host))
  final def db: DB = connection(dbName)
}

abstract class BaseMongoDao[Model <: BaseModel[ID], ID](private val collectionName: String) extends BaseDao[Model] {
  protected def collection = BaseMongoDao.db.collection[JSONCollection](collectionName)

  override def add(model: Model): Future[Model] = {
    collection.insert(model.toJson)
    Future[Model] {model}
  }
}