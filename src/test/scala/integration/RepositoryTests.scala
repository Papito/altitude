package integration

import org.scalatest.Matchers._
import altitude.models.Repository
import org.scalatest.DoNotDiscover
import altitude.Util

@DoNotDiscover class RepositoryTests (val config: Map[String, String]) extends IntegrationTestCore {

  test("create repository") {
    val r = Repository(
      name = Util.randomStr(),
      rootFolderId = Util.randomStr(),
      uncatFolderId = Util.randomStr()
    )

    val r1: Repository = altitude.service.repository.add(r)
    val r2: Repository = altitude.service.repository.getById(r1.id.get)
    r2.name should be(r.name)
  }
}
