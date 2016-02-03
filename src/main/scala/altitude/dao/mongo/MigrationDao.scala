package altitude.dao.mongo

import altitude.Altitude
import altitude.transactions.TransactionId
import com.mongodb.DBObject
import org.slf4j.LoggerFactory

class MigrationDao(app: Altitude) extends altitude.dao.MigrationDao(app) {
  private final val log = LoggerFactory.getLogger(getClass)

  override def currentVersion(implicit txId: TransactionId = new TransactionId): Int = {
    val version_col = BaseMongoDao.DB.get("ver")

    val res: Option[DBObject] = version_col.findOne()

    res.isDefined match {
      case true => {
        val rec = res.get
        val version = rec.get("db_version")
        version.asInstanceOf[Int]
      }
      case false => 0
    }
  }

  def executeCommand(command: String)(implicit txId: TransactionId): Unit = {
    val res = BaseMongoDao.DB.get.eval(command)
  }

  def versionUp(implicit txId: TransactionId = new TransactionId): Unit = Unit
}
