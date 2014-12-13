package controllers.manager.api

import akka.actor.{Props, ActorRef, Actor}
import play.api.Play
import play.api.Play.current
import util.log
import org.json4s.native.Serialization.write
import org.json4s.DefaultFormats
import play.api.mvc._
import constants.{const => C}

object ImportController extends Controller {
   implicit val formats = DefaultFormats

    def index = Action { implicit request =>
      log.debug("Import API controller", C.tag.API)
      val importPath = Play.current.configuration.getString("import.path").getOrElse("")
      val assets = global.ManagerGlobal.importService.getImportAssets(path = importPath)
      val out = assets map {_.toDict}

      Ok( write( "assets" -> out) )
    }

  object ImportWebSocketActor {
    def props(out: ActorRef) = Props(new ImportWebSocketActor(out))
  }

  class ImportWebSocketActor(out: ActorRef) extends Actor {
    def receive = {
      case msg: String => cycle(out)
    }
  }

  def socket = WebSocket.acceptWithActor[String, String] { request => out =>
    ImportWebSocketActor.props(out)
  }

  private def cycle(out: ActorRef): Unit = {
    log.debug("Import API controller", C.tag.API)
    val importPath = Play.current.configuration.getString("import.path").getOrElse("")
    val assets = global.ManagerGlobal.importService.getImportAssets(path = importPath)
    val data = assets map {_.toDict}
    assets.foreach(asset => out ! (asset.toDict.toString()))
  }


}
