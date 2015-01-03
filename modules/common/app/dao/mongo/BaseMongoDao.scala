package dao.mongo

import altitude.{Const => C}
import dao.BaseDao
import models.BaseModel
import play.api.Play
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api._
import util.log

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
object BaseMongoDao {
  private val host = Play.current.configuration.getString("mongo.host").getOrElse("")
  require(host.nonEmpty)
  private val dbName = Play.current.configuration.getString("mongo.name").getOrElse("")
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
    log.info("MONGO INSERT")
    val f = collection.insert(model.toJson)
    Await.result(f, 1.second)
    Future[Model] {model}
  }
}