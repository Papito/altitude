package controllers.api

import akka.actor.{Actor, ActorRef, Props}
import altitude.models.{FileImportAsset, Asset}
import altitude.services.AbstractTransactionManager
import altitude.Util.log
import altitude.{Const => C}
import global.Altitude
import org.json4s.DefaultFormats
import org.json4s.native.Serialization._
import play.api.Play
import play.api.Play.current
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import net.codingwell.scalaguice.InjectorExtensions._

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
    lazy val app: Altitude = Altitude.getInstance()
    val txManager = app.injector.instance[AbstractTransactionManager]
    val importPath = Play.current.configuration.getString("import.path").getOrElse("")
    val assets = app.service.fileImport.getFilesToImport(path=importPath).toList
    val assetsIt = assets.toIterator

    def receive = {
      case "next" => out ! (if (assetsIt.hasNext) {
        val importAsset: FileImportAsset = assetsIt.next()
        val assetF = app.service.fileImport.importAsset(importAsset)
        write("asset" -> assetF.map{asset => asset})
      } else "")
      case "total" => out ! write("total" -> assets.size)
    }

    override def postStop() = {
      log.info("Socket closed", C.tag.WEB)
    }
  }
}