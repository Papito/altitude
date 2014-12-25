 package global

import scala.concurrent.Future
import play.api._
import play.api.mvc.Results._
import play.api.mvc._
import util.log
import constants.{const => C}

import service.manager.{MetadataService, FileImportService}

object ManagerGlobal extends GlobalSettings {	

  object service {
    val importService = new FileImportService
    val metadatService = new MetadataService
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
