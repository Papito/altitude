package controllers.web

import altitude.util.log
import altitude.{Const => C}
import play.api.mvc._

object IndexController extends Controller {

	def index = Action { implicit request =>
    log.info("Index controller", C.tag.WEB)
		Ok(views.html.index())
	}

}
