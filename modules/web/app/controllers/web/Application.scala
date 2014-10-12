package controllers.web

import models._
import play.api._
import play.api.mvc._
import play.api.Play.current

object Application extends Controller {

	def index = Action { implicit request =>
		Ok(views.html.web.index("Hello! I'm the WEB!"))
	}

}
