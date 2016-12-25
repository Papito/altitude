package integration

import altitude.Util
import altitude.models.{BaseModel, Repository}
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._

@DoNotDiscover class RepositoryTests (val config: Map[String, String]) extends IntegrationTestCore {

  test("create repository") {
    val r = Repository(
      name = Util.randomStr(),
      rootFolderId = BaseModel.genId,
      uncatFolderId = BaseModel.genId
    )

    val r1: Repository = altitude.service.repository.add(r)
    val r2: Repository = altitude.service.repository.getById(r1.id.get)
    r2.name shouldEqual r.name
  }
}
