package controllers.web

import play.api.mvc._
import altitude.util.log
import altitude.{Const => C}

object ImportController extends Controller {

	def index = Action { implicit request =>
    log.info("Import controller", C.tag.WEB)
		Ok(views.html.load())
	}

}
