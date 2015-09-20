package integration

import altitude.{Configuration, Altitude}
import altitude.dao.mongo.BaseMongoDao
import com.mongodb.casbah.MongoClient
import org.scalatest.BeforeAndAfterAll

class MongoSuite extends AllTests(config = Map("datasource" -> "mongo")) with BeforeAndAfterAll {

  override def afterAll(): Unit = BaseMongoDao.CLIENT.get.close()
}