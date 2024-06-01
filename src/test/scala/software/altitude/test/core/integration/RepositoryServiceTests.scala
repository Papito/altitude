package software.altitude.test.core.integration

import org.scalatest.DoNotDiscover
import org.scalatest.matchers.must.Matchers.{contain, not}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import software.altitude.core.models.Repository
import software.altitude.core.{Util, Const => C}

@DoNotDiscover class RepositoryServiceTests(val config: Map[String, Any]) extends IntegrationTestCore {

  test("create repository") {
    val repo: Repository = altitude.service.repository.addRepository(
      name = Util.randomStr(),
      fileStoreType = C.FileStoreType.FS,
      user = this.currentUser)

    repo.fileStoreConfig.keys should contain(C.Repository.Config.PATH)

    val storedRepo: Repository = altitude.service.repository.getRepositoryById(repo.id.get)
    storedRepo.name shouldEqual repo.name
    storedRepo.fileStoreConfig.keys should contain(C.Repository.Config.PATH)
    storedRepo.createdAt should not be None
  }
}
