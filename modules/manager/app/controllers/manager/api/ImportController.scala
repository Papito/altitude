package controllers.manager.api

import akka.actor.{Props, ActorRef, Actor}
import play.api.Play
import play.api.Play.current
import util.log
import play.api.mvc._
import constants.{const => C}

object ImportController extends Controller {

  def socket = WebSocket.acceptWithActor[String, String] { request => out =>
    log.info("Import websocket endpoint", C.tag.WEB)
    ImportWebSocketActor.props(out)
  }

  private object ImportWebSocketActor {
    def props(out: ActorRef) = Props(new ImportWebSocketActor(out))
  }

  private class ImportWebSocketActor(out: ActorRef) extends Actor {
    val importPath = Play.current.configuration.getString("import.path").getOrElse("")
    val assets = global.ManagerGlobal.importService.iterateAssets(path=importPath)

    def receive = {
      case msg: String => {
        if (assets.hasNext)
          out ! assets.next().toDict.toString
        else out ! ""
      }
    }
  }

}
