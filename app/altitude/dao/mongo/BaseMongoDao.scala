package altitude.dao.mongo

import altitude.dao.{BaseDao, TransactionId}
import altitude.models.BaseModel
import altitude.util.log
import altitude.{Const => C}
import org.joda.time.DateTime
import play.api.Play
import play.api.libs.json._
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api._
import reactivemongo.bson.BSONDateTime
import reactivemongo.core.commands.LastError

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import altitude.Util
import play.modules.reactivemongo.json.BSONFormats._

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

  override def add(jsonIn: JsObject)(implicit txId: TransactionId): Future[JsObject] = {
    log.debug(s"Starting database INSERT for: $jsonIn", C.tag.DB)

    // append core attributes
    val json: JsObject = jsonIn ++ JsObject(Seq(
      C.Base.ID -> JsString(BaseModel.genId),
      C.Base.CREATED_AT ->  Json.obj("$date" -> Util.utcNow)
    ))

    println("!!!!!")
    println(json)
    val f: Future[LastError] = collection.insert(json)
    f map {res => if (res.ok) json else throw res.getCause}
  }

  override def getById(id: String)(implicit txId: TransactionId): Future[JsObject] = {
    log.debug(s"Getting by ID '$id'", C.tag.DB)

    val query = Json.obj(C.Base.ID -> id)
    val cursor: Cursor[JsObject] = collection.find(query).cursor[JsObject]
    val f: Future[List[JsObject]] = cursor.collect[List](upTo = 2)

    f map { results =>
      log.debug(s"Found ${results.length} records", C.tag.DB)
      if (results.length > 1) throw new Exception("getById should return only a single result")
      results.head
    }
  }
}