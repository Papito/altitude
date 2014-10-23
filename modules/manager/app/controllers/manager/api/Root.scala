package controllers.manager.api

import models._
import play.api._
import play.api.libs.json._
import play.api.mvc._
import play.api.Play.current

object Root extends Controller {

    def index = Action { implicit request => 
        Ok( Json.obj("assets" -> List[String]()) )
    }

}
