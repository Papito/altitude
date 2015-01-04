package dao.common.mongo

import altitude.{Const => C}
import dao.common.BaseDao
import models.common.BaseModel
import play.api.Play
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api._
import reactivemongo.core.commands.LastError
import util.log

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object BaseMongoDao {
  lazy val host = Play.current.configuration.getString("mongo.host").getOrElse("")
  require(host.nonEmpty)
  lazy val dbName = Play.current.configuration.getString("mongo.name").getOrElse("")
  require(dbName.nonEmpty)

  log.info("Initializing mongo connection",
    Map("host" -> host, "dbName" -> dbName), C.tag.DB, C.tag.APP)

  final private val driver = new MongoDriver
  final private val connection = driver.connection(List(host))
  final def db: DB = connection(dbName)
}

abstract class BaseMongoDao[Model <: BaseModel[ID], ID](private val collectionName: String) extends BaseDao[Model] {
  protected def collection = BaseMongoDao.db.collection[JSONCollection](collectionName)

  override def add(model: Model): Future[Model] = {
    log.debug("Starting database INSERT for: $o", Map("o" -> model.toJson))
    val f: Future[LastError] = collection.insert(model.toJson)
    f map {res => if (res.ok) model else throw res.getCause}
  }
}