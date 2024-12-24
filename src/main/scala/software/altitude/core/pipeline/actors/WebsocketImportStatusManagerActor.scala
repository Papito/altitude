package software.altitude.core.pipeline.actors

import org.scalatra.atmosphere.{AtmosphereClient, TextMessage}
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import software.altitude.core.AltitudeActorSystem

import scala.concurrent.ExecutionContext.Implicits.global

object WebsocketImportStatusManagerActor {
  sealed trait Command
  final case class AddClient(userId: String, client: AtmosphereClient) extends AltitudeActorSystem.Command with Command
  final case class UserWideImportStatus(userId: String, message: String) extends AltitudeActorSystem.Command with Command
  final case class RemoveClient(userId: String, client: AtmosphereClient) extends AltitudeActorSystem.Command with Command

  private val successStatusTickerTemplate = "<div id=\"statusText\">%s</div>"
  private val warningStatusTickerTemplate = "<div id=\"statusText\" class=\"warning\">%s</div>"
  private val errorStatusTickerTemplate = "<div id=\"statusText\" class=\"error\">%s</div>"

  def apply(): Behavior[Command] = Behaviors.setup(context => new WebsocketImportStatusManagerActor(context))
}

class WebsocketImportStatusManagerActor(context: ActorContext[WebsocketImportStatusManagerActor.Command])
  extends AbstractBehavior[WebsocketImportStatusManagerActor.Command](context) {

  import WebsocketImportStatusManagerActor._

  private val userToWsClientLookup = collection.mutable.Map[String, List[AtmosphereClient]]()

  override def onMessage(msg: WebsocketImportStatusManagerActor.Command): Behavior[WebsocketImportStatusManagerActor.Command] = {
    msg match {
      case AddClient(userId, client) =>
        context.log.info(s"Adding client $client for user $userId")
        val clients = userToWsClientLookup.getOrElse(userId, List())
        userToWsClientLookup.update(userId, client :: clients)
        Behaviors.same

      case UserWideImportStatus(userId, message) =>
        context.log.info(s"Sending message to WS clients for user $userId")
        userToWsClientLookup.get(userId).foreach { clients =>
          clients.foreach { client =>
            context.log.info(s"Sending message to client $client")
            client.send(TextMessage(message))
          }
        }
        Behaviors.same

      case RemoveClient(userId, client) =>
        context.log.info(s"Removing client $client for user $userId")
        userToWsClientLookup.get(userId).foreach { clients =>
          userToWsClientLookup.update(userId, clients.filterNot(_ == client))
        }
        Behaviors.same
    }
  }
}
