package integration

import altitude.Altitude
import altitude.dao.mongo.BaseMongoDao
import org.scalatest.BeforeAndAfterAll

class MongoSuite extends AllTests(config = Map("datasource" -> "mongo")) with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    // delete all test databases created in this suite
    val altitude: Altitude = new Altitude(additionalConfiguration = config)
    val client = BaseMongoDao.client(altitude)
    val thisDbName = altitude.config.getString("db.mongo.db")

    client.databaseNames().
      filter({_.startsWith("altitude-test-")}).
      filter(_ != thisDbName).foreach {dbName: String =>
        println(s"Deleting $dbName")
        client.dropDatabase(dbName)
    }
    client.close()
  }
}