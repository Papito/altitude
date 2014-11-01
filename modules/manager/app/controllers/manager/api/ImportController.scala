package controllers.manager.api

import util.log
import service.manager.ImportService
import play.api.libs.json._
import play.api.mvc._

object ImportController extends Controller {

    private val importService: ImportService = new ImportService

    def index = Action { implicit request =>
      log.debug("Import API controller", log.API)

      val assets = this.importService.getImportAssets

      Ok( Json.obj("ok" -> JsBoolean(true)) )
    }
}
