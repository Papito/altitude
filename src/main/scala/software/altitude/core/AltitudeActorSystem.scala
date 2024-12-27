package software.altitude.core

import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.PostStop
import org.apache.pekko.actor.typed.Signal
import org.apache.pekko.actor.typed.scaladsl.AbstractBehavior
import org.apache.pekko.actor.typed.scaladsl.ActorContext
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import software.altitude.core.actors.ImportStatusWsActor

object AltitudeActorSystem {
  trait Command

  def apply(): Behavior[Command] = Behaviors.setup(context => new AltitudeActorSystem(context))
}

private class AltitudeActorSystem(context: ActorContext[AltitudeActorSystem.Command]) extends AbstractBehavior[AltitudeActorSystem.Command](context) {

  private val websocketImportStatusManagerActor = context.spawn(ImportStatusWsActor(), "importStatusWsActor")

  override def onMessage(msg: AltitudeActorSystem.Command): Behavior[AltitudeActorSystem.Command] = {
    msg match {
      case command: ImportStatusWsActor.Command =>
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
