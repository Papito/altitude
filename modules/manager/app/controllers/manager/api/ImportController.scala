package controllers.manager.api

import util.log
import service.manager.ImportService
import org.json4s.native.Serialization.write
import org.json4s.DefaultFormats
import play.api.mvc._

object ImportController extends Controller {

    private val importService = new ImportService
    implicit val formats = DefaultFormats

    def index = Action { implicit request =>
      log.debug("Import API controller", log.API)

      val assets = this.importService.getImportAssets
      val out = assets map {_.toDict}

      Ok( write( "assets" -> out) )
    }
}
