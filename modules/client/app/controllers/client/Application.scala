package controllers.client

import play.api.mvc._

object Application extends Controller {

	def index = Action { implicit request =>
		Ok(views.html.client.index("Hello! I'm the CLIENT!"))
	}

}
