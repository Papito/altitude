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

    // create id UNLESS specified
    val id: String = (jsonIn \ C.Base.ID).asOpt[String] match {
      case Some(id: String) => id
      case _ => BaseModel.genId
    }

    val createdAt = Util.utcNow
    val origObj: DBObject =  com.mongodb.util.JSON.parse(
      jsonIn.toString()).asInstanceOf[DBObject]
    val coreAttrObj = MongoDBObject("_id" -> id, C.Base.CREATED_AT -> createdAt)
    val obj: DBObject = origObj ++ coreAttrObj

    COLLECTION.insert(obj)

    jsonIn ++ Json.obj(
      C.Base.ID -> JsString(id),
      C.Base.CREATED_AT -> Util.isoDateTime(Some(Util.utcNow))
    )
  }

  override def deleteByQuery(q: Query)(implicit txId: TransactionId): Int = {
    val query = fixMongoQuery(q)
    val mongoQuery: DBObject = query.params
    log.info(mongoQuery.toString)
    val res = COLLECTION.remove(mongoQuery)
    val numDeleted = res.getN
    log.debug(s"Deleted records: $numDeleted")
    numDeleted
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

    val cursor: MongoCursor = query.rpp match {
      case 0 => COLLECTION.find(mongoQuery)
      case _ => COLLECTION.find(mongoQuery).skip((query.page - 1) * query.rpp).limit(query.rpp)
    }

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
  protected final def fixMongoFields(json: JsObject): JsObject = json ++ Json.obj(
      C.Base.ID -> (json \ "_id").as[String],
      C.Base.CREATED_AT ->  (json \ C.Base.CREATED_AT \ "$date").asOpt[String],
      C.Base.UPDATED_AT -> (json \ C.Base.UPDATED_AT \ "$date").asOpt[String]
    )

  protected final def fixMongoQuery(q: Query): Query = {
    // _id -> id
    q.params.contains("id")  match {
      case false => q
      case true => {
        val id = q.params.get("id").get
        val params = (q.params - "id") ++ Map("_id" -> id)
        Query(params = params, rpp = q.rpp, page = q.page)
      }
    }
  }

  override def updateByQuery(q: Query, json: JsObject, fields: List[String])(implicit txId: TransactionId): Int = {
    log.debug(s"Updating with data $json for $q")

    val query  = fixMongoQuery(q)
    val mongoQuery: DBObject = query.params

    // combine the selected fields we want to update from the JSON repr of the mode, with updated_at
    val updateJson = JsObject(
      json.fieldSet.filter {v: (String, JsValue) => fields.contains(v._1)}.toSeq) ++ Json.obj(
        C.Base.UPDATED_AT -> Util.isoDateTime(Some(Util.utcNow))
    )

    val o: DBObject =  MongoDBObject(
      "$set" -> com.mongodb.util.JSON.parse(updateJson.toString()).asInstanceOf[DBObject]
    )

    log.debug(s"Updating with data $updateJson for $mongoQuery")

    val res: WriteResult = COLLECTION.update(mongoQuery, o)
    val updated = res.getN
    log.debug(s"Updated $updated records")
    updated
  }
}