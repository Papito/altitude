package controllers.api

import akka.actor.{Actor, ActorRef, Props}
import altitude.util.log
import altitude.{Const => C}
import global.Altitude
import org.json4s.DefaultFormats
import org.json4s.native.Serialization._
import play.api.Play
import play.api.Play.current
import play.api.libs.json.JsValue
import play.api.mvc._

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
    val assets = Altitude.getInstance().service.fileImport.getFilesToImport(path=importPath).toList
    val assetsIt = assets.toIterator

    def receive = {
      case "next" => out ! (if (assetsIt.hasNext) write("asset" -> (assetsIt.next(): JsValue)) else "")
      case "total" => out ! write("total" -> assets.size)
    }

    override def postStop() = {
      log.info("Socket closed", C.tag.WEB)
    }
  }
}