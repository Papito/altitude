package controllers.manager.api

import util.log
import play.api.libs.json._
import play.api.mvc._
import constants.{const => C}

object RootController extends Controller {

    def index = Action { implicit request =>
      log.debug("Root API controller", C.tag.API)
      Ok( Json.obj() )
    }
}
