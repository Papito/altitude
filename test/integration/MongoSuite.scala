package integration

import org.scalatest.DoNotDiscover

@DoNotDiscover class MongoSuite extends AllTests(Map("datasource" -> "mongo"))