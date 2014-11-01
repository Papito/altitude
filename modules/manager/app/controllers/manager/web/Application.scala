package controllers.manager.web

import models._
import play.api._
import play.api.mvc._
import util.log
import play.api.Play.current

object Application extends Controller {

	def index = Action { implicit request =>
    log.info("Manager index controller", log.WEB)
		Ok(views.html.manager.index())
	}

}
