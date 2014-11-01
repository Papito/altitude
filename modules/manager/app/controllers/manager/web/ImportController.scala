package controllers.manager.web

import play.api.mvc._
import util.log

object ImportController extends Controller {

	def index = Action { implicit request =>
    log.info("Import controller", log.WEB)
		Ok(views.html.manager.load())
	}

}
