package altitude.transactions

import altitude.Altitude
import altitude.dao.mongo.BaseMongoDao
import org.slf4j.LoggerFactory

class MongoTransactionManager(app: Altitude) extends VoidTransactionManager(app) {
  private final val log = LoggerFactory.getLogger(getClass)

  override def freeResources(): Unit = {
    if (BaseMongoDao.CLIENT.isDefined) {
      log.info("Closing MONGO client")
      BaseMongoDao.CLIENT.get.close()
    }
    log.info("Freeing transaction list")
  }

}
