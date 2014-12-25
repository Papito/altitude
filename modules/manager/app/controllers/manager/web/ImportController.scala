package controllers.manager.web

import constants.{const => C}
import play.api.mvc._
import util.log

object ImportController extends Controller {

	def index = Action { implicit request =>
    log.info("Import controller", C.tag.WEB)
		Ok(views.html.manager.load())
	}

}
