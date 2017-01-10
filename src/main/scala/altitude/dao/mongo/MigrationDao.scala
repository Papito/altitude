package altitude.dao.mongo

import altitude.transactions.TransactionId
import altitude.{Altitude, Context}
import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import org.slf4j.LoggerFactory

class MigrationDao(val app: Altitude) extends altitude.dao.MigrationDao {
  private final val log = LoggerFactory.getLogger(getClass)

  override def currentVersion(implicit ctx: Context, txId: TransactionId ): Int = {
    val system = BaseMongoDao.DB.get("system")

    val res: Option[DBObject] = system.findOne()

    res.isDefined match {
      case true =>
        val rec = res.get
        val version = rec.get("version")
        version.asInstanceOf[Int]
      case false => 0
    }
  }

  override def executeCommand(command: String)(implicit ctx: Context, txId: TransactionId): Unit = {
    val res = BaseMongoDao.DB.get.eval(command)
  }

  override def versionUp()(implicit ctx: Context, txId: TransactionId): Unit = {
    val o: DBObject =  MongoDBObject(
      "$set" -> MongoDBObject("version" -> 1)
    )

    val system = BaseMongoDao.DB.get("system")
    system.update(MongoDBObject(), o, upsert = true)
  }
}
