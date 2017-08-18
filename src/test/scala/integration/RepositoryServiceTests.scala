package integration

import software.altitude.core.models.{BaseModel, Repository}
import software.altitude.core.{Const => C}
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._
import software.altitude.core.Util

@DoNotDiscover class RepositoryServiceTests (val config: Map[String, Any]) extends IntegrationTestCore {

  test("create repository") {
    val r = Repository(
      name = Util.randomStr(),
      rootFolderId = BaseModel.genId,
      triageFolderId = BaseModel.genId,
      fileStoreType = C.FileStoreType.FS,
      fileStoreConfig = Map()
    )

    val r1: Repository = altitude.service.repository.add(r)
    val r2: Repository = altitude.service.repository.getById(r1.id.get)
    r2.name shouldEqual r.name
  }
}
