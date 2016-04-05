package altitude.dao.mongo

import altitude.Altitude
import altitude.transactions.TransactionId
import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import org.slf4j.LoggerFactory

class MigrationDao(app: Altitude) extends altitude.dao.MigrationDao(app) {
  private final val log = LoggerFactory.getLogger(getClass)

  override def currentVersion(implicit txId: TransactionId = new TransactionId): Int = {
    val system = BaseMongoDao.DB.get("system")

    val res: Option[DBObject] = system.findOne()

    res.isDefined match {
      case true => {
        val rec = res.get
        val version = rec.get("version")
        version.asInstanceOf[Int]
      }
      case false => 0
    }
  }

  override def executeCommand(command: String)(implicit txId: TransactionId): Unit = {
    val res = BaseMongoDao.DB.get.eval(command)
  }

  override def versionUp()(implicit txId: TransactionId = new TransactionId): Unit = {
    val o: DBObject =  MongoDBObject(
      "$set" -> MongoDBObject("version" -> 1)
    )

    val system = BaseMongoDao.DB.get("system")
    system.update(MongoDBObject(), o, upsert = true)
  }
}
