package software.altitude.core.controllers

import org.scalatra.scalate.ScalateSupport
import software.altitude.core.Const
import software.altitude.core.Environment
import software.altitude.core.RequestContext
import software.altitude.core.models.Repository
import software.altitude.core.models.User
import software.altitude.core.util.Query

class BaseWebController extends BaseController with ScalateSupport {
  before() {
    Environment.CURRENT match {
      case Environment.Name.DEV | Environment.Name.PROD =>
        val repoResults = app.service.repository.query(new Query())
        if (repoResults.records.nonEmpty) {
          RequestContext.repository.value = Some(repoResults.records.head: Repository)
          // logger.warn(s"Using first found repository: ${RequestContext.repository.value.get.name}")
        }

        val userResults = app.service.user.query(new Query())
        if (userResults.records.nonEmpty) {
          RequestContext.account.value = Some(userResults.records.head: User)
          // logger.warn(s"Using first found user: ${RequestContext.account.value.get.email}")
        }

      case Environment.Name.TEST =>
        // logger.info("TEST AUTHENTICATION VIA HEADER")

        val testRepoId: String = request.getHeader(Const.Api.REPO_TEST_HEADER_ID)
        if (testRepoId != null) {
          val repo: Repository = app.service.repository.getById(testRepoId)
          RequestContext.repository.value = Some(repo)
        }

        val testUserId: String = request.getHeader(Const.Api.USER_TEST_HEADER_ID)
        if (testUserId != null) {
          val user: User = app.service.user.getById(testUserId)
          RequestContext.account.value = Some(user)
          logger.info(RequestContext.account.value.toString)
        }

      case _ => throw new RuntimeException("Unknown environment")
    }

  }
}
