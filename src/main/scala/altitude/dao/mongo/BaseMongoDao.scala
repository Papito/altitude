package altitude.dao.mongo

import altitude.dao.BaseDao
import altitude.models.BaseModel
import altitude.models.search.Query
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C, Util}
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.gridfs.JodaGridFS
import org.slf4j.LoggerFactory
import play.api.libs.json._

object BaseMongoDao {
  import com.mongodb.casbah.commons.conversions.scala._
  RegisterJodaTimeConversionHelpers()

  // let each application in the JVM have its own client
  var CLIENTS = scala.collection.mutable.Map[Int, MongoClient]()

  def client(app: Altitude): MongoClient = {
    BaseMongoDao.CLIENTS.contains(app.id) match {
      case true => BaseMongoDao.CLIENTS.get(app.id).get
      case false => {
        // create the client (and connection pool for this app once)

        val host: String = app.config.getString("db.mongo.host")
        val dbPort: Int = Integer.parseInt(app.config.getString("db.mongo.port"))
        println(s"Creating mongo client for app ${app.id}")
        def client = MongoClient(host, dbPort)
        // save the client for this app once
        BaseMongoDao.CLIENTS += (app.id -> client)
        client
      }
    }
  }

  def removeClient(app: Altitude) = {
    val client = BaseMongoDao.CLIENTS.get(app.id)
    if (client.isDefined) {
      println(s"Closing mongo client for app ${app.id}")
      client.get.close()
    }
    println(s"Removing mongo client for app ${app.id}")
    BaseMongoDao.CLIENTS.remove(app.id)
  }

  var GRID_FSs = scala.collection.mutable.Map[String, JodaGridFS]()

  def gridFS(app: Altitude, db: MongoDB, colName: String): JodaGridFS = {
    val gridFsId = s"${app.id}-$colName"

    BaseMongoDao.GRID_FSs.contains(gridFsId) match {
      case true => BaseMongoDao.GRID_FSs.get(gridFsId).get
      case false => {
        println(s"Creating preview gridFS for app ${app.id}")
        val gridFs = JodaGridFS(db, colName)
        GRID_FSs += (gridFsId -> gridFs)
        gridFs
      }
    }
  }

  def removeGridFS(app: Altitude, colName: String): Unit = {
    val gridFsId = s"${app.id}-$colName"
    println(s"Removing preview gridFS for app ${app.id}")
    BaseMongoDao.GRID_FSs.remove(gridFsId)
  }
}

abstract class BaseMongoDao(protected val collectionName: String) extends BaseDao {
  val log =  LoggerFactory.getLogger(getClass)

  private val DB_NAME: String = app.config.getString("db.mongo.db")
  protected def DB = BaseMongoDao.client(app)(DB_NAME)
  protected def COLLECTION: MongoCollection = DB(collectionName)

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