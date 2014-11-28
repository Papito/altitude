package controllers.manager.web

import play.api.mvc._
import util.log
import constants.{const => C}

object ImportController extends Controller {

	def index = Action { implicit request =>
    log.info("Import controller", C.tag.WEB)
		Ok(views.html.manager.load())
	}

}
