package software.altitude.core.actors

import org.apache.pekko.actor.typed.{ActorRef, Behavior, Scheduler}
import org.apache.pekko.actor.typed.scaladsl.AbstractBehavior
import org.apache.pekko.actor.typed.scaladsl.ActorContext
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.util.Timeout
import software.altitude.core.AltitudeActorSystem
import software.altitude.core.actors.FaceRecModelActor.{FacePrediction, ModelLabels, ModelSize}
import software.altitude.core.models.Face

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object FaceRecManagerActor {
  sealed trait Command
  final case class AddFace(repositoryId: String, face: Face, personLabel: Int, replyTo: ActorRef[AltitudeActorSystem.EmptyResponse]) extends AltitudeActorSystem.Command with Command
  final case class Initialize(repositoryId: String, replyTo: ActorRef[AltitudeActorSystem.EmptyResponse]) extends AltitudeActorSystem.Command with Command
  final case class Predict(repositoryId: String, face: Face, replyTo: ActorRef[FacePrediction]) extends AltitudeActorSystem.Command with Command
  final case class GetModelSize(repositoryId: String, replyTo: ActorRef[ModelSize]) extends AltitudeActorSystem.Command with Command
  final case class GetModelLabels(repositoryId: String, replyTo: ActorRef[ModelLabels]) extends AltitudeActorSystem.Command with Command

  def apply(): Behavior[Command] = Behaviors.setup(context => new FaceRecManagerActor(context))
}

class FaceRecManagerActor(context: ActorContext[FaceRecManagerActor.Command]) extends AbstractBehavior[FaceRecManagerActor.Command](context) {
  import FaceRecManagerActor._

  private var modelActors = Map.empty[String, ActorRef[FaceRecModelActor.Command]]

  implicit val timeout: Timeout = 3.seconds
  implicit val scheduler: Scheduler = context.system.scheduler
  implicit val ec: ExecutionContext = context.executionContext

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case Initialize(repositoryId, replyTo) =>
        val modelActor = modelActors.getOrElse(repositoryId, context.spawn(FaceRecModelActor(), s"faceRecModelActor-$repositoryId"))
        modelActors += (repositoryId -> modelActor)

        println("Asking 2")
        modelActor.ask(FaceRecModelActor.Initialize(_)).onComplete {
          case Success(response) => {
            println("Responded 2")
            replyTo ! response
          }
          case Failure(exception) => context.log.error("Failed to initialize model actor", exception)
        }(ec)
        Behaviors.same

      case AddFace(repositoryId, face, personLabel, replyTo) =>
        modelActors.get(repositoryId) match {
          case Some(modelActor) =>
            modelActor.ask(FaceRecModelActor.AddFace(face, personLabel, _)).mapTo[AltitudeActorSystem.EmptyResponse].onComplete {
              case Success(response) => replyTo ! response
              case Failure(exception) => context.log.error("Failed to add face", exception)
            }(ExecutionContext.global)
            Behaviors.same
          case None =>
            throw new RuntimeException(s"No model actor found for repositoryId: $repositoryId")
        }

      case Predict(repositoryId, face, replyTo) =>
        modelActors.get(repositoryId) match {
          case Some(modelActor) =>
            modelActor.ask(FaceRecModelActor.Predict(face, _)).mapTo[FacePrediction].onComplete {
              case Success(response) => replyTo ! response
              case Failure(exception) => context.log.error("Failed to predict face", exception)
            }(ExecutionContext.global)
            Behaviors.same
          case None =>
            throw new RuntimeException(s"No model actor found for repositoryId: $repositoryId")
        }

      case GetModelSize(repositoryId, replyTo) =>
        modelActors.get(repositoryId) match {
          case Some(modelActor) =>
            modelActor.ask(FaceRecModelActor.GetModelSize(_)).mapTo[ModelSize].onComplete {
              case Success(response) => replyTo ! response
              case Failure(exception) => context.log.error("Failed to get model size", exception)
            }(ExecutionContext.global)
            Behaviors.same
          case None =>
            throw new RuntimeException(s"No model actor found for repositoryId: $repositoryId")
        }

      case GetModelLabels(repositoryId, replyTo) =>
        modelActors.get(repositoryId) match {
          case Some(modelActor) =>
            modelActor.ask(FaceRecModelActor.GetModelLabels(_)).mapTo[ModelLabels].onComplete {
              case Success(response) => replyTo ! response
              case Failure(exception) => context.log.error("Failed to get model labels", exception)
            }(ExecutionContext.global)
            Behaviors.same
          case None =>
            throw new RuntimeException(s"No model actor found for repositoryId: $repositoryId")
        }
    }
  }
}
