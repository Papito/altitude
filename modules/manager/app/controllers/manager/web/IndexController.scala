package controllers.manager.web

import play.api.mvc._
import util.log
import constants.{const => C}

object IndexController extends Controller {

	def index = Action { implicit request =>
    log.info("Index controller", C.tag.WEB)
		Ok(views.html.manager.index())
	}

}
