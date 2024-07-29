package software.altitude.core.auth.strategies

import org.scalatra.ScalatraBase
import org.scalatra.auth.ScentryStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.altitude.core.{AltitudeServletContext, RequestContext}
import software.altitude.core.models.User
import software.altitude.core.util.Query

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class LocalDevRememberMeStrategy(protected val app: ScalatraBase)(implicit request: HttpServletRequest, response: HttpServletResponse)
  extends ScentryStrategy[User] {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  override def name: String = "RememberMe"

  def authenticate()(implicit request: HttpServletRequest, response: HttpServletResponse): Option[User] = {
//    if (Environment.CURRENT != Environment.Name.DEV)
//      throw new RuntimeException("LocalDevRememberMeStrategy can only be used in development environment")

    val userResults = AltitudeServletContext.app.service.user.query(new Query())

    if (userResults.records.nonEmpty) {
      RequestContext.account.value = Some(userResults.records.head: User)
    }

    RequestContext.account.value match {
      case Some(user) => Some(user)
      case None => None
    }
  }
}
