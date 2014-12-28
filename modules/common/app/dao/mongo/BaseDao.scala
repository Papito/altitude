package dao.mongo

import reactivemongo.api._
import util.log

import scala.concurrent.ExecutionContext.Implicits.global

abstract class BaseDao extends dao.BaseDao {
  private val driver = new MongoDriver
  protected var mongoConn: MongoConnection = null

  protected val COLLECTION_NAME: String
  protected val collection: Collection = db(COLLECTION_NAME)

  protected def conn: MongoConnection = {
    if (mongoConn == null) {
      log.info("Connecting to Mongo DB")
      mongoConn = driver.connection(List("localhost"))
    }
    mongoConn
  }

  protected def db: DefaultDB = conn("altitide")
}
