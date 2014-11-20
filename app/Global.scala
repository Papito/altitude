import global.ManagerGlobal
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent.Future

object Global extends GlobalSettings {
	
	private def getSubdomain (request: RequestHeader) = request.domain.replaceFirst("[\\.]?[^\\.]+[\\.][^\\.]+$", "")
	
	override def onRouteRequest (request: RequestHeader) = getSubdomain(request) match {
		case "web" => client.Routes.routes.lift(request)
		case _ => manager.Routes.routes.lift(request)
	}
	
	// 404 - page not found error
	override def onHandlerNotFound (request: RequestHeader) = getSubdomain(request) match {
		case "web" => ClientGlobal.onHandlerNotFound(request)
		case _ => ManagerGlobal.onHandlerNotFound(request)
	}
	
	// 500 - internal server error
	override def onError (request: RequestHeader, throwable: Throwable) = getSubdomain(request) match {
		case "web" => ClientGlobal.onError(request, throwable)
		case _ => ManagerGlobal.onError(request, throwable)
	}
	
	// called when a route is found, but it was not possible to bind the request parameters
	override def onBadRequest (request: RequestHeader, error: String) = getSubdomain(request) match {
		case "web" => ClientGlobal.onBadRequest(request, error)
		case _ => ManagerGlobal.onBadRequest(request, error)
	}

}
