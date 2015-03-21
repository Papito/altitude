package altitude.dao.mongo

import altitude.dao.{Transaction, BaseDao}
import altitude.util.log
import altitude.{Const => C}
import play.api.Play
import play.api.libs.json.{JsObject, JsValue, Json}
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api._
import reactivemongo.core.commands.LastError

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

abstract class BaseMongoDao(private val collectionName: String) extends BaseDao {
  protected def collection = BaseMongoDao.db.collection[JSONCollection](collectionName)

  override def add(json: JsValue)(implicit tx: Option[Transaction]): Future[JsValue] = {
    log.debug("Starting database INSERT for: $o", Map("o" -> json))
    val f: Future[LastError] = collection.insert(json)
    f map {res => if (res.ok) json else throw res.getCause}
  }

  override def getById(id: String)(implicit tx: Option[Transaction]): Future[JsValue] = {
    log.debug("Getting by ID '$id'", Map(C.Common.ID -> id))

    val query = Json.obj(C.Common.ID -> id)
    val cursor: Cursor[JsObject] = collection.find(query).cursor[JsObject]
    val f: Future[List[JsObject]] = cursor.collect[List](upTo = 2)

    f map {results =>
      if (results.length == 1) {
       results.head
      }
      else {
        throw new Exception("getById should return only a single result")}
      }
  }
}