import play.api._
import play.api.mvc._

object Global extends GlobalSettings {
	
	private def getSubdomain (request: RequestHeader) = request.domain.replaceFirst("[\\.]?[^\\.]+[\\.][^\\.]+$", "")
	
	override def onRouteRequest (request: RequestHeader) = getSubdomain(request) match {
		case "web" => client.Routes.routes.lift(request)
		case _ => manager.Routes.routes.lift(request)
	}
	
	// 404 - page not found error
	override def onHandlerNotFound (request: RequestHeader) = getSubdomain(request) match {
		case "web" => global.client.Global.onHandlerNotFound(request)
		case _ => global.manager.Global.onHandlerNotFound(request)
	}
	
	// 500 - internal server error
	override def onError (request: RequestHeader, throwable: Throwable) = getSubdomain(request) match {
		case "web" => global.client.Global.onError(request, throwable)
		case _ => global.manager.Global.onError(request, throwable)
	}
	
	// called when a route is found, but it was not possible to bind the request parameters
	override def onBadRequest (request: RequestHeader, error: String) = getSubdomain(request) match {
		case "web" => global.client.Global.onBadRequest(request, error)
		case _ => global.manager.Global.onBadRequest(request, error)
	}

}
