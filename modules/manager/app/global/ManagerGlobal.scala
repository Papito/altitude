 package global

import altitude.{Const => C}
import com.google.inject.{Injector, AbstractModule, Guice}
import dao.manager.LibraryDao
import net.codingwell.scalaguice.ScalaModule
import play.api._
import play.api.mvc.Results._
import play.api.mvc._
import service.manager.{FileImportService, _}
import util.log

import scala.concurrent.Future

object ManagerGlobal extends GlobalSettings {
  var injector: Injector = null

  class InjectionModule extends AbstractModule with ScalaModule  {
    override def configure(): Unit = {
      val dataSourceType = Play.current.configuration.getString("db.dataSource").getOrElse("")
      log.info("Datasource type: $source", Map("source" -> dataSourceType), C.tag.APP)
      log.info("Application configure", C.tag.APP)
      dataSourceType match {
        case "mongo" => bind[LibraryDao].toInstance(new dao.manager.mongo.LibraryDao)
        case _ => throw new IllegalArgumentException("Do not know of datasource: " + dataSourceType); sys.exit()
      }
    }
  }

  object service {
    val fileImport: FileImportService = new FileImportService
    val metadata: AbstractMetadataService = new TikaMetadataService
    val library: LibraryService = new LibraryService
  }

	override def onStart(app: Application) {
    injector = Guice.createInjector(new InjectionModule)
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
