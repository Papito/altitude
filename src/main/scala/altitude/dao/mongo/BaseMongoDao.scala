package altitude.dao.mongo

import altitude.dao.BaseDao
import altitude.models.BaseModel
import altitude.models.search.Query
import altitude.transactions.TransactionId
import altitude.{Const => C, Environment, Util, Configuration}
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoClient
import org.slf4j.LoggerFactory
import play.api.libs.json._

object BaseMongoDao {
  import com.mongodb.casbah.commons.conversions.scala._
  RegisterJodaTimeConversionHelpers()

  private val config = new Configuration()
  private val host: String = config.getString("db.mongo.host")
  private val dbPort: Int = Integer.parseInt(config.getString("db.mongo.port"))
  private val dataSource = config.getString("datasource")

  // create mongo client if it's the datasource, or we are in the test harness
  val CLIENT: Option[MongoClient] =
    if (dataSource == "mongo" || Environment.ENV == Environment.TEST) {
      Some(MongoClient(host, dbPort))
    } else None

  private val DB_NAME: String = config.getString("db.mongo.db")
  def DB = CLIENT match {
    case None => None
    case _ => Some(CLIENT.get(DB_NAME))
  }
}

abstract class BaseMongoDao(protected val collectionName: String) extends BaseDao {
  private final val log = LoggerFactory.getLogger(getClass)

  protected def COLLECTION: MongoCollection = BaseMongoDao.DB.get(collectionName)

  override def add(jsonIn: JsObject)(implicit txId: TransactionId): JsObject = {
    log.debug(s"Starting database INSERT for: $jsonIn")

    // create DBObject for insert with input data  + required core attributes
    val id: String = BaseModel.genId
    val createdAt = Util.utcNow
    val origObj: DBObject =  com.mongodb.util.JSON.parse(
      jsonIn.toString()).asInstanceOf[DBObject]
    val coreAttrObj = MongoDBObject("id" -> id, "_id" -> id, C.Base.CREATED_AT -> createdAt)
    val obj: DBObject = origObj ++ coreAttrObj

    COLLECTION.insert(obj)
    
    jsonIn ++ Json.obj(
      C.Base.ID -> JsString(id),
      C.Base.CREATED_AT -> Util.isoDateTime(Some(Util.utcNow))
    )
  }

  override def getById(id: String)(implicit txId: TransactionId): Option[JsObject] = {
    log.debug(s"Getting by ID '$id'", C.LogTag.DB)

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
    val cursor: MongoCursor = COLLECTION.find(mongoQuery).limit(query.rpp)
    log.debug(s"Found ${cursor.length} records")

    // iterate through results and "fix" mongo fields
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