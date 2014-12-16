package controllers.manager.api

import akka.actor.{Props, ActorRef, Actor}
import play.api.Play
import play.api.Play.current
import util.log
import play.api.mvc._
import constants.{const => C}
import org.json4s.native.Serialization._
import org.json4s.DefaultFormats

object ImportController extends Controller {
  implicit val formats = DefaultFormats

  def socket = WebSocket.acceptWithActor[String, String] { request => out =>
    log.info("Import websocket endpoint", C.tag.WEB)
    ImportWebSocketActor.props(out)
  }

  private object ImportWebSocketActor {
    def props(out: ActorRef) = Props(new ImportWebSocketActor(out))
  }

  private class ImportWebSocketActor(out: ActorRef) extends Actor {
    val importPath = Play.current.configuration.getString("import.path").getOrElse("")
    val assets = global.ManagerGlobal.importService.getAssetsToImport(path=importPath).toList
    val assetsIt = assets.toIterator

    def receive = {
      case "next" => out ! (if (assetsIt.hasNext) write("asset" -> assetsIt.next().toMap) else "")
      case "total" => out ! write("total" -> assets.size)
    }

    override def postStop() = {
      log.info("Socket closed", C.tag.WEB)
    }
  }

}