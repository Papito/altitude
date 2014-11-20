package controllers.manager.api

import play.api.Play
import util.log
import service.manager.ImportService
import org.json4s.native.Serialization.write
import org.json4s.DefaultFormats
import play.api.mvc._

object ImportController extends Controller {
    implicit val formats = DefaultFormats

    def index = Action { implicit request =>
      log.debug("Import API controller", log.API)
      val importPath = Play.current.configuration.getString("import.path").getOrElse("")
      val assets = global.ManagerGlobal.importService.getImportAssets(path = importPath)
      val out = assets map {_.toDict}

      Ok( write( "assets" -> out) )
    }
}
