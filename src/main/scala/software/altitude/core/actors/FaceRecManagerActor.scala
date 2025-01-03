package software.altitude.core.actors

import org.apache.pekko.actor.typed.{ActorRef, Behavior, Scheduler}
import org.apache.pekko.actor.typed.scaladsl.AbstractBehavior
import org.apache.pekko.actor.typed.scaladsl.ActorContext
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.util.Timeout
import software.altitude.core.AltitudeActorSystem
import software.altitude.core.models.Face

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object FaceRecManagerActor {
  sealed trait Command
  final case class AddFace(repositoryId: String, face: Face, personLabel: Int, replyTo: ActorRef[AltitudeActorSystem.EmptyResponse]) extends AltitudeActorSystem.Command with Command
  final case class Initialize(repositoryId: String, replyTo: ActorRef[AltitudeActorSystem.EmptyResponse]) extends AltitudeActorSystem.Command with Command
  final case class Clear(repositoryId: String, replyTo: ActorRef[AltitudeActorSystem.EmptyResponse]) extends AltitudeActorSystem.Command with Command

  def apply(): Behavior[Command] = Behaviors.setup(context => new FaceRecManagerActor(context))
}

class FaceRecManagerActor(context: ActorContext[FaceRecManagerActor.Command]) extends AbstractBehavior[FaceRecManagerActor.Command](context) {
  import FaceRecManagerActor._

  private var modelActors = Map.empty[String, ActorRef[FaceRecModelActor.Command]]

  implicit val timeout: Timeout = 3.seconds
  implicit val scheduler: Scheduler = context.system.scheduler

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case Initialize(repositoryId, replyTo) =>
        val modelActor = modelActors.getOrElse(repositoryId, context.spawn(FaceRecModelActor(), s"faceRecModelActor-$repositoryId"))
        modelActors += (repositoryId -> modelActor)
        val futureResponse =  modelActor.ask[AltitudeActorSystem.EmptyResponse](replyTo => FaceRecModelActor.Initialize(replyTo))
        Await.result(futureResponse, timeout.duration)
        replyTo ! AltitudeActorSystem.EmptyResponse()

        println(modelActors.size)

        Behaviors.same

      case AddFace(repositoryId, face, personLabel, replyTo) =>
        modelActors.get(repositoryId) match {
          case Some(modelActor) =>
            // modelActor ! FaceRecModelActor.AddFace(face, personLabel)
          case None =>
            context.log.error(s"No model actor found for repositoryId: $repositoryId")
        }
        Behaviors.same
    }
  }
}
