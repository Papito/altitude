package global

import altitude.util.log
import altitude.{Const => C}
import play.api._
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.Future

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    log.info("Application $app starting", Map("app" -> app.hashCode()), C.tag.APP)
    Altitude.register(app)
  }

  override def onStop(app: Application) {
    log.info("Application $app stopping", Map("app" -> app.hashCode()), C.tag.APP)
    Altitude.deregister(app)
  }

  // 404 - page not found error
  override def onHandlerNotFound (request: RequestHeader) = Future.successful(
    NotFound(views.html.errors.onHandlerNotFound(request))
  )

  // 500 - internal server error
  override def onError (request: RequestHeader, throwable: Throwable) = Future.successful(
    InternalServerError(views.html.errors.onError(throwable))
  )

  // called when a route is found, but it was not possible to bind the request parameters
  override def onBadRequest (request: RequestHeader, error: String) = Future.successful(
    BadRequest("Bad Request: " + error)
  )
}
