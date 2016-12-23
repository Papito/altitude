package altitude.dao.mongo

import altitude.dao.BaseDao
import altitude.models.BaseModel
import altitude.models.search.{Query, QueryResult}
import altitude.{Configuration, Const => C, Context, Environment, Util}
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
      val client = Some(MongoClient(host, dbPort))
      client
    } else {
      None
    }

  private val DB_NAME: String = config.getString("db.mongo.db")
  def DB = CLIENT match {
    case None => None
    case _ => Some(CLIENT.get(DB_NAME))
  }
}

abstract class BaseMongoDao(protected val collectionName: String) extends BaseDao {
  private final val log = LoggerFactory.getLogger(getClass)

  protected def COLLECTION: MongoCollection = BaseMongoDao.DB.get(collectionName)

  protected lazy val MONGO_QUERY_BUILDER = new MongoQueryBuilder(COLLECTION)

  override def add(jsonIn: JsObject)(implicit ctx: Context): JsObject = {
    log.debug(s"Starting database INSERT for: $jsonIn")

    val o = makeObjectForInsert(jsonIn)
    val id = o.get("_id").asInstanceOf[String]

    COLLECTION.insert(o)
    log.debug(s"INSERTING: $o")

    jsonIn ++ Json.obj(
      C.Base.ID -> JsString(id))
  }

  protected def makeObjectForInsert(jsonIn: JsObject)(implicit ctx: Context): DBObject = {
    // create id UNLESS specified
    val id: String = (jsonIn \ C.Base.ID).asOpt[String] match {
      case Some(id: String) => id
      case _ => BaseModel.genId
    }

    verifyId(id)

    val createdAt = Util.utcNowNoTZ
    val origObj: DBObject =  com.mongodb.util.JSON.parse(jsonIn.toString()).asInstanceOf[DBObject]

    val coreAttrObj = MongoDBObject(
        "_id" -> id,
        C.Base.REPO_ID -> ctx.repo.id.get,
        C.Base.CREATED_AT -> createdAt)
      origObj ++ coreAttrObj
  }

  override def deleteByQuery(q: Query)(implicit ctx: Context): Int = {
    val query = fixMongoQuery(q)
    val mongoQuery: DBObject = query.params
    log.info(mongoQuery.toString)
    val res = COLLECTION.remove(mongoQuery)
    val numDeleted = res.getN
    log.debug(s"Deleted records: $numDeleted")
    numDeleted
  }

  override def getById(id: String)(implicit ctx: Context): Option[JsObject] = {
    log.debug(s"Getting by ID '$id'", C.LogTag.DB)

    val q = MongoDBObject(
      "_id" -> id,
      C.Base.REPO_ID -> ctx.repo.id.get)

    val o: Option[DBObject] = COLLECTION.findOne(q)

    log.debug(s"RETRIEVED object: $o", C.LogTag.DB)

    o.isDefined match {
      case false => None
      case true =>
        val json = Json.parse(o.get.toString).as[JsObject]
        Some(fixMongoFields(json))
    }
  }

  override def query(query: Query)(implicit ctx: Context): QueryResult = {
    val cursor: MongoCursor = MONGO_QUERY_BUILDER.toSelectCursor(query)

    log.debug(s"Found ${cursor.length} records")

    // iterate through results and "fix" mongo fields
    val records = cursor.map(o => {
      val json: JsObject = Json.parse(o.toString).as[JsObject]
      fixMongoFields(json)
    }).toList

    QueryResult(records = records, total = cursor.count(), query = Some(query))
  }

  /*
  Return a JSON record with timestamp and ID fields translated from Mongo's "extended" format
   */
  protected def fixMongoFields(json: JsObject): JsObject = {
    val out = json ++ Json.obj(
      C.Base.CREATED_AT ->  (json \ C.Base.CREATED_AT \ "$date").asOpt[String],
      C.Base.UPDATED_AT -> (json \ C.Base.UPDATED_AT \ "$date").asOpt[String]
    )

    json.keys.contains(C.Base.ID) match  {
      case true => out ++ Json.obj(C.Base.ID -> (json \ "_id").as[String])
      case false => out
    }
  }

  protected final def fixMongoQuery(q: Query)(implicit ctx: Context): Query = {
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

  override def getByIds(ids: Set[String])
                       (implicit ctx: Context): List[JsObject] = {
    val query: DBObject =  MongoDBObject(
      C.Base.REPO_ID -> ctx.repo.id.get,
      "_id" -> MongoDBObject("$in" -> ids)
    )

    val cursor: MongoCursor = COLLECTION.find(query)

    // iterate through results and "fix" mongo fields
    cursor.map(o => {
      val json: JsObject = Json.parse(o.toString).as[JsObject]
      fixMongoFields(json)
    }).toList
  }

  override def updateByQuery(q: Query, json: JsObject, fields: List[String])
                            (implicit ctx: Context): Int = {
    log.debug(s"Updating with data $json for $q")

    val query  = fixMongoQuery(q)
    val mongoQuery: DBObject = query.params ++ MongoDBObject(C.Base.REPO_ID -> ctx.repo.id.get)

    // combine the selected fields we want to update from the JSON repr of the model, with updated_at
    val updateJson = JsObject(
      json.fieldSet.filter {v: (String, JsValue) => fields.contains(v._1)}.toSeq) ++ Json.obj(
      C.Base.UPDATED_AT -> Util.isoDateTime(Some(Util.utcNowNoTZ))
    )

    val o: DBObject =  MongoDBObject(
      "$set" -> MongoDBObject(updateJson.toString())
    )

    log.debug(s"Updating with data $updateJson for $mongoQuery")

    val res: WriteResult = COLLECTION.update(mongoQuery, o)
    val updated = res.getN
    log.debug(s"Updated $updated records")
    updated
  }

  override def increment(id: String, field: String, count: Int = 1)
                        (implicit ctx: Context) = {
    val query: DBObject =  MongoDBObject(
      "_id" -> id,
      C.Base.REPO_ID -> ctx.repo.id.get)

    val o: DBObject =  MongoDBObject(
      "$inc" -> MongoDBObject(field -> count)
    )

    COLLECTION.update(query, o)
  }

  override def decrement(id: String,  field: String, count: Int = 1)
                        (implicit ctx: Context) = {
    increment(id, field, -count)
  }
}