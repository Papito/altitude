package altitude.dao.mongo

import altitude.dao.BaseDao
import altitude.models.BaseModel
import altitude.models.search.Query
import altitude.transactions.TransactionId
import altitude.{Const => C, Util}
import com.mongodb.casbah.Imports._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import play.api.libs.json._


abstract class BaseMongoDao(private val collectionName: String) extends BaseDao {
  import com.mongodb.casbah.commons.conversions.scala._
  RegisterJodaTimeConversionHelpers()

  val log =  LoggerFactory.getLogger(getClass)
  private val host: String = app.config.get("db.mongo.host")
  private val dbPort: Int = Integer.parseInt(app.config.get("db.mongo.port"))
  private val client = MongoClient(host, dbPort)

  private val dbName: String = app.config.get("db.mongo.db")
  protected val db = client(dbName)
  protected val collection: MongoCollection = db(collectionName)

  override def add(jsonIn: JsObject)(implicit txId: TransactionId): JsObject = {
    log.debug(s"Starting database INSERT for: $jsonIn")

    // append core attributes
    val id: String = BaseModel.genId
    val createdAt = Util.utcNow

    val obj: DBObject =  com.mongodb.util.JSON.parse(jsonIn.toString()).asInstanceOf[DBObject] ++
      MongoDBObject("id" -> id, "_id" -> id, C.Base.CREATED_AT -> createdAt)

    collection.insert(obj)

    val json: JsObject = jsonIn ++ JsObject(Seq(
      "id" -> JsString(id),
      C.Base.CREATED_AT -> JsString(createdAt.toString)
    ))
    json
  }

  override def getById(id: String)(implicit txId: TransactionId): Option[JsObject] = {
    log.debug(s"Getting by ID '$id'", C.tag.DB)

    val o: Option[DBObject] = collection findOneByID new ObjectId(id)

    o.isDefined match {
      case false => None
      case true =>
        val json: JsObject = Json.parse(o.toString).as[JsObject]
        Some(fixMongoFields(json))
    }
  }

  override def query(query: Query)(implicit txId: TransactionId): List[JsObject] = {
    val mongoQuery: DBObject = query.params
    val cursor: MongoCursor = collection.find(mongoQuery).limit(1000) //FIXME: setting
    log.debug(s"Found ${cursor.length} records")

    cursor.map(o => {
      val json: JsObject = Json.parse(o.toString).as[JsObject]
      fixMongoFields(json)
    }).toList
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