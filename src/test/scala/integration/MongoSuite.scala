package integration

import altitude.{Configuration, Altitude}
import altitude.dao.mongo.BaseMongoDao
import com.mongodb.casbah.MongoClient
import org.scalatest.BeforeAndAfterAll

class MongoSuite extends AllTests(config = Map("datasource" -> "mongo")) with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    // delete all test databases created in this suite
    def client = BaseMongoDao.CLIENT.get

    client.databaseNames().
      filter({_.startsWith("altitude-test-")}).foreach {dbName: String =>
        println(s"Deleting $dbName")
        client.dropDatabase(dbName)
    }
    client.close()
  }
}