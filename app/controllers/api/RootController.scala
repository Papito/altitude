package controllers.api

import altitude.Util.log
import altitude.{Const => C}
import play.api.libs.json._
import play.api.mvc._

object RootController extends Controller {

    def index = Action { implicit request =>
      log.debug("Root API controller", C.tag.API)
      Ok( Json.obj() )
    }
}
