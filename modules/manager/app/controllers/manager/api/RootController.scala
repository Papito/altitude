package controllers.manager.api

import util.log
import play.api.libs.json._
import play.api.mvc._
import play.api.Play.current

object RootController extends Controller {

    def index = Action { implicit request =>
      log.debug("Root API controller", log.API)
      Ok( Json.obj() )
    }
}
