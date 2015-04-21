package controllers.web

import altitude.Util.log
import altitude.{Const => C}
import play.api.mvc._

object ImportController extends Controller {

	def index = Action { implicit request =>
    log.info("Import controller", C.tag.WEB)
		Ok(views.html.load())
	}

}
