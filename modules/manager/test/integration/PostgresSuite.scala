package integration

import org.scalatest.DoNotDiscover

@DoNotDiscover class PostgresSuite extends AllTests(Map("datasource" -> "postgres"))