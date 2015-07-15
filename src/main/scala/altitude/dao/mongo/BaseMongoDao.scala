package altitude.dao.mongo

import altitude.dao.BaseDao
import altitude.models.BaseModel
import altitude.models.search.Query
import altitude.transactions.TransactionId
import altitude.{Const => C, Altitude, Util}
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoClient
import org.slf4j.LoggerFactory
import play.api.libs.json._

object BaseMongoDao {
  // let each application in the JVM have its own client
  var CLIENTS = scala.collection.mutable.Map[Int, MongoClient]()
  protected def client(app: Altitude): MongoClient = {
    if (BaseMongoDao.CLIENTS.contains(app.id)) {
      return BaseMongoDao.CLIENTS.get(app.id).get
    }

    // create the client (and connection pool for this app once)
    val host: String = app.config.getString("db.mongo.host")
    val dbPort: Int = Integer.parseInt(app.config.getString("db.mongo.port"))
    def client = MongoClient(host, dbPort)
    // save the client for this app once
    BaseMongoDao.CLIENTS += (app.id -> client)
    client
  }
}

abstract class BaseMongoDao(private val collectionName: String) extends BaseDao {
  import com.mongodb.casbah.commons.conversions.scala._
  RegisterJodaTimeConversionHelpers()

  val log =  LoggerFactory.getLogger(getClass)
  private val DB_NAME: String = app.config.getString("db.mongo.db")
  protected def DB = BaseMongoDao.client(app)(DB_NAME)
  protected def COLLECTION: MongoCollection = DB(collectionName)
  protected val MAX_RECORDS = app.config.getInt("db.max_records")

  override def add(jsonIn: JsObject)(implicit txId: TransactionId): JsObject = {
    log.debug(s"Starting database INSERT for: $jsonIn")

    // append core attributes
    val id: String = BaseModel.genId
    val createdAt = Util.utcNow

    val obj: DBObject =  com.mongodb.util.JSON.parse(jsonIn.toString()).asInstanceOf[DBObject] ++
      MongoDBObject("id" -> id, "_id" -> id, C.Base.CREATED_AT -> createdAt)

    COLLECTION.insert(obj)
    
    jsonIn ++ Json.obj(
      C.Base.ID -> JsString(id),
      C.Base.CREATED_AT -> Util.isoDateTime(Some(Util.utcNow))
    )
  }

  override def getById(id: String)(implicit txId: TransactionId): Option[JsObject] = {
    log.debug(s"Getting by ID '$id'", C.tag.DB)

    val o: Option[DBObject] = COLLECTION.findOneByID(id)

    o.isDefined match {
      case false => None
      case true =>
        val json = Json.parse(o.get.toString).as[JsObject]
        Some(fixMongoFields(json))
    }
  }

  override def query(query: Query)(implicit txId: TransactionId): List[JsObject] = {
    val mongoQuery: DBObject = query.params
    val cursor: MongoCursor = COLLECTION.find(mongoQuery).limit(MAX_RECORDS)
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
    json ++ Json.obj(
      C.Base.ID -> (json \ "_id").as[String],
      C.Base.CREATED_AT ->  (json \ C.Base.CREATED_AT \ "$date").asOpt[String],
      C.Base.UPDATED_AT -> (json \ C.Base.UPDATED_AT \ "$date").asOpt[String]
    )
  }
}