package controllers.api

import altitude.util.log
import play.api.libs.json._
import play.api.mvc._
import altitude.{Const => C}

object RootController extends Controller {

    def index = Action { implicit request =>
      log.debug("Root API controller", C.tag.API)
      Ok( Json.obj() )
    }
}
