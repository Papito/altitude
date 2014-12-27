 package global

import constants.{const => C}
import play.api._
import play.api.mvc.Results._
import play.api.mvc._
import service.manager.{FileImportService, AbstractMetadataService, TikaMetadataService}
import util.log

import scala.concurrent.Future

object ManagerGlobal extends GlobalSettings {	

  object service {
    val importService: FileImportService = new FileImportService
    val metadata: AbstractMetadataService = new TikaMetadataService
  }

	override def onStart(app: Application) {
		log.info("Application starting", C.tag.APP)
	}

	// 404 - page not found error
	override def onHandlerNotFound (request: RequestHeader) = Future.successful(
		NotFound(views.html.manager.errors.onHandlerNotFound(request))
	)
	
	// 500 - internal server error
	override def onError (request: RequestHeader, throwable: Throwable) = Future.successful(
		InternalServerError(views.html.manager.errors.onError(throwable))
	)
	
	// called when a route is found, but it was not possible to bind the request parameters
	override def onBadRequest (request: RequestHeader, error: String) = Future.successful(
		BadRequest("Bad Request: " + error)
	)
}
