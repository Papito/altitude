package integration

import altitude.dao.mongo.BaseMongoDao
import org.scalatest.BeforeAndAfterAll

class MongoSuite extends AllTests(config = Map("datasource" -> "mongo")) with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    if (BaseMongoDao.CLIENT.isDefined) {
      BaseMongoDao.CLIENT.get.close()
    }
  }
}