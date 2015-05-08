package altitude.dao.mongo

import altitude.Util.log
import altitude.dao.{BaseDao, TransactionId}
import altitude.models.BaseModel
import altitude.models.search.Query
import altitude.{Const => C, Util}
import org.joda.time.DateTime
import play.api.Play
import play.api.libs.json._
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

  override def add(jsonIn: JsObject)(implicit txId: TransactionId): Future[JsObject] = {
    log.debug(s"Starting database INSERT for: $jsonIn", C.tag.DB)

    // append core attributes
    val id = BaseModel.genId
    val createdAt = Util.utcNow
    val json: JsObject = jsonIn ++ JsObject(Seq(
      "_id" -> Json.obj("$oid" -> id),
      C.Base.CREATED_AT -> Json.obj("$date" -> createdAt)
    ))

    val f: Future[LastError] = collection.insert(json)
    f map {res =>
      if (res.ok) {
        fixMongoFields(json)
      }
      else throw res.getCause}
  }

  override def getById(id: String)(implicit txId: TransactionId): Future[Option[JsObject]] = {
    log.debug(s"Getting by ID '$id'", C.tag.DB)

    val mongoQuery = JsObject(Seq("_id" -> Json.obj("$oid" -> id)))

    val cursor: Cursor[JsObject] = collection.find(mongoQuery).cursor[JsObject]
    val f: Future[List[JsObject]] = cursor.collect[List](upTo = 2)

    f map { results =>
      log.debug(s"Found ${results.length} records", C.tag.DB)

      if (results.length > 1) throw new Exception("getById should return only a single result")

      if (results.length == 0) {
        Future{None}
      }

      val res = fixMongoFields(results.head)
      Some(res)
    }
  }

  override def query(query: Query)(implicit txId: TransactionId): Future[List[JsObject]] = {
    val mongoQuery = query.params map { case (key, value) => (key, JsString(value.toString))}

    val cursor: Cursor[JsObject] = collection.find(mongoQuery).cursor[JsObject]
    val f: Future[List[JsObject]] = cursor.collect[List](upTo = 1000) //FIXME: setting

    f map { results =>
      log.debug(s"Found ${results.length} records", C.tag.DB)
      results.map(fixMongoFields)
    }
  }

  /*
  Return a JSON record with timestamp and ID fields translated from Mongo's "extended" format
   */
  protected def fixMongoFields(json: JsObject): JsObject = {
    val createdAtMillis: Option[Long] =  (json \ C.Base.CREATED_AT \ "$date").asOpt[Long]
    val createdAt: Option[DateTime] = {
      if (createdAtMillis.isDefined) Some(new DateTime(createdAtMillis.get)) else None
    }

    val updatedAtMillis: Option[Long] =  (json \ C.Base.UPDATED_AT \ "$date").asOpt[Long]
    val updatedAt: Option[DateTime] = {
      if (updatedAtMillis.isDefined) Some(new DateTime(updatedAtMillis.get)) else None
    }

    json ++ Json.obj(
      C.Base.ID -> (json \ "_id" \ "$oid").as[String],
      C.Base.CREATED_AT -> {createdAt match {
        case None => JsNull
        case _ => Util.isoDateTime(createdAt)
      }},
      C.Base.UPDATED_AT -> {updatedAt match {
        case None => JsNull
        case _ => Util.isoDateTime(updatedAt)
      }}
    )
  }
}