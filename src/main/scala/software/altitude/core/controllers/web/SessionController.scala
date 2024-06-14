package software.altitude.core.controllers.web

import org.scalatra.ScalatraServlet
import org.scalatra.scalate.ScalateSupport
import software.altitude.core.auth.AuthenticationSupport

class SessionController extends ScalatraServlet with ScalateSupport with AuthenticationSupport {

  before("/new") {
    logger.info("SessionsController: checking whether to run RememberMeStrategy: " + !isAuthenticated)

    if (!isAuthenticated) {
      scentry.authenticate("RememberMe")
    }
  }

  get("/new") {
    if (isAuthenticated) redirect("/")

    contentType = "text/html"
    mustache("login")
  }

  post("/") {
    scentry.authenticate()

    if (isAuthenticated) {
      redirect("/")
    } else {
      mustache("login")
    }
  }

  // Never do this in a real app. State changes should never happen as a result of a GET request. However, this does
  // make it easier to illustrate the logout code.
  get("/logout") {
    scentry.logout()
    redirect("/")
  }

}
