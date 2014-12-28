package dao.mongo

import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api._
import reactivemongo.api.DefaultDB
import scala.concurrent.ExecutionContext.Implicits.global

import util.log

abstract class BaseDao extends dao.BaseDao {
  private val driver = new MongoDriver

  protected val COLLECTION_NAME: String
  protected def collection: JSONCollection  = db.collection[JSONCollection](COLLECTION_NAME)

  protected def conn: MongoConnection = driver.connection(List("localhost"))

  protected def db: DefaultDB = conn("altitude")
}
