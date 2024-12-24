package software.altitude.core

import org.apache.pekko.actor.typed.{ActorSystem, Behavior, PostStop, Signal}
import org.apache.pekko.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import software.altitude.core.pipeline.actors.WebsocketImportStatusManagerActor

object AltitudeActorSystem {
  trait Command

  def apply(): Behavior[Command] = Behaviors.setup(context => new AltitudeActorSystem(context))
}

private class AltitudeActorSystem(context: ActorContext[AltitudeActorSystem.Command])
  extends AbstractBehavior[AltitudeActorSystem.Command](context) {

  private val websocketImportStatusManagerActor = context.spawn(WebsocketImportStatusManagerActor(), "websocketImportStatusManagerActor")

  override def onMessage(msg: AltitudeActorSystem.Command): Behavior[AltitudeActorSystem.Command] = {
    msg match {
      case command: WebsocketImportStatusManagerActor.Command =>
        websocketImportStatusManagerActor ! command
        Behaviors.same
      case _ =>
        Behaviors.unhandled
    }
  }
  override def onSignal: PartialFunction[Signal, Behavior[AltitudeActorSystem.Command]] = {
    case PostStop =>
        context.log.info("Actor system stopped")
      this
  }

  context.log.info("Actor system started")
}
