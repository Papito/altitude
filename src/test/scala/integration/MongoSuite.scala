package integration

import altitude.Altitude
import altitude.dao.mongo.BaseMongoDao
import org.scalatest.BeforeAndAfterAll

object MongoSuite {
  val app = new Altitude(Map("datasource" -> "mongo"))
}

class MongoSuite extends AllTests(config = Map("datasource" -> "mongo")) with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    if (BaseMongoDao.CLIENT.isDefined) {
      BaseMongoDao.CLIENT.get.close()
    }
  }
}