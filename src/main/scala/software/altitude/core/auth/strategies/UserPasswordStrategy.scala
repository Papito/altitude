package software.altitude.core.auth.strategies

import org.scalatra.ScalatraBase
import org.scalatra.auth.ScentryStrategy
import org.slf4j.{Logger, LoggerFactory}
import software.altitude.core.models.User

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

class UserPasswordStrategy(protected val app: ScalatraBase)(implicit request: HttpServletRequest, response: HttpServletResponse)
  extends ScentryStrategy[User] {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  override def name: String = "UserPassword"

  private def login = app.params.getOrElse("login", "")
  private def password = app.params.getOrElse("password", "")

  /**
   * * Determine whether the strategy should be run for the current request.
   */
  override def isValid(implicit request: HttpServletRequest): Boolean = {
    logger.info("UserPasswordStrategy: determining isValid: " + (login != "" && password != "").toString)
    login != "" && password != ""
  }

  /**
   * In real life, this is where we'd consult our data store, asking it whether the user credentials matched any existing user. Here, we'll
   * just check for a known login/password combination and return a user if it's found.
   */
  def authenticate()(implicit request: HttpServletRequest, response: HttpServletResponse): Option[User] = {
    logger.info("UserPasswordStrategy: attempting authentication")

    if (login == "admin" && password == "admin") {
      logger.info("UserPasswordStrategy: login succeeded")
      Some(User(Some("myfakeid")))
    } else {
      logger.info("UserPasswordStrategy: login failed")
      None
    }
  }

  /** What should happen if the user is currently not authenticated? */
  override def unauthenticated()(implicit request: HttpServletRequest, response: HttpServletResponse): Unit = {
    app.redirect("/session/new")
  }

}
