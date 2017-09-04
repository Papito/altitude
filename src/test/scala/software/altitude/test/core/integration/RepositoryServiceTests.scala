package software.altitude.test.core.integration

import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._
import software.altitude.core.models.{BaseModel, Repository}
import software.altitude.core.{Const => C, Util}

@DoNotDiscover class RepositoryServiceTests (val config: Map[String, Any]) extends IntegrationTestCore {

  test("create repository") {
    val r1: Repository = altitude.service.repository.addRepository(
      name = Util.randomStr(),
      fileStoreType = C.FileStoreType.FS,
      user = this.currentUser)

    r1.fileStoreConfig.keys should contain(C.Repository.Config.PATH)

    val r2: Repository = altitude.service.repository.getRepositoryById(r1.id.get)
    r2.name shouldEqual r1.name
    r2.fileStoreConfig.keys should contain(C.Repository.Config.PATH)
    r2.createdAt should not be None
  }
}
