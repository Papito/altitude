package controllers.web

import play.api.mvc._
import altitude.util.log
import altitude.{Const => C}

object IndexController extends Controller {

	def index = Action { implicit request =>
    log.info("Index controller", C.tag.WEB)
		Ok(views.html.index())
	}

}
