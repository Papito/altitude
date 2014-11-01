package controllers.manager.web

import play.api.mvc._
import util.log

object IndexController extends Controller {

	def index = Action { implicit request =>
    log.info("Index controller", log.WEB)
		Ok(views.html.manager.index())
	}

}
