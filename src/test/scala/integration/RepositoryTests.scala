package integration

import org.scalatest.Matchers._
import altitude.models.{BaseModel, Repository}
import org.scalatest.DoNotDiscover
import altitude.Util

@DoNotDiscover class RepositoryTests (val config: Map[String, String]) extends IntegrationTestCore {

  test("create repository") {
    val r = Repository(
      name = Util.randomStr(),
      // FIXME: these will be done by the service
      rootFolderId = Some(BaseModel.genId),
      uncatFolderId = Some(BaseModel.genId)
    )

    val r1: Repository = altitude.service.repository.add(r)
    val r2: Repository = altitude.service.repository.getById(r1.id.get)
    r2.name should be(r.name)
  }
}