package software.altitude.core

import org.apache.pekko.actor.typed.{ActorRef, Behavior, PostStop, Scheduler, Signal}
import org.apache.pekko.actor.typed.scaladsl.AbstractBehavior
import org.apache.pekko.actor.typed.scaladsl.ActorContext
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.util.Timeout
import software.altitude.core.actors.FaceRecManagerActor
import software.altitude.core.actors.ImportStatusWsActor

import scala.concurrent.duration.DurationInt

object AltitudeActorSystem {
  trait Command
  final case class EmptyResponse() extends Command
  def apply(): Behavior[Command] = Behaviors.setup(context => new AltitudeActorSystem(context))
}

private class AltitudeActorSystem(context: ActorContext[AltitudeActorSystem.Command]) extends AbstractBehavior[AltitudeActorSystem.Command](context) {

  private val websocketImportStatusManagerActor = context.spawn(ImportStatusWsActor(), "importStatusWsActor")
  private val faceRecManagerActor = context.spawn(FaceRecManagerActor(), "faceRecManagerActor")

  implicit val timeout: Timeout = 3.seconds
  implicit val scheduler: Scheduler = context.system.scheduler

  /** Route messages to the appropriate actors and actor managers */
  override def onMessage(msg: AltitudeActorSystem.Command): Behavior[AltitudeActorSystem.Command] = {
    msg match {
      case command: ImportStatusWsActor.Command =>
        websocketImportStatusManagerActor ! command
        Behaviors.same
      case command: FaceRecManagerActor.Initialize =>
        faceRecManagerActor.ask[AltitudeActorSystem.EmptyResponse](replyTo => FaceRecManagerActor.Initialize(command.repositoryId, replyTo))
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
