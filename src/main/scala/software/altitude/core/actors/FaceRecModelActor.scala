package software.altitude.core.actors

import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.face.LBPHFaceRecognizer
import software.altitude.core.AltitudeActorSystem
import software.altitude.core.models.Face
import software.altitude.core.util.ImageUtil.matFromBytes

import java.util

object FaceRecModelActor {
  sealed trait Command
  final case class AddFace(face: Face, personLabel: Int, replyTo: ActorRef[AltitudeActorSystem.EmptyResponse]) extends Command
  final case class Initialize(replyTo: ActorRef[AltitudeActorSystem.EmptyResponse]) extends Command

  def apply(): Behavior[Command] = Behaviors.setup(context => new FaceRecModelActor(context))
}

class FaceRecModelActor(context: ActorContext[FaceRecModelActor.Command]) extends AbstractBehavior[FaceRecModelActor.Command](context) {
  import FaceRecModelActor._

  val recognizer: LBPHFaceRecognizer = LBPHFaceRecognizer.create()
  recognizer.setGridX(10)
  recognizer.setGridY(10)
  recognizer.setRadius(2)

  initialize()

  private def initialize(): Unit = {
    recognizer.clear()

    val initialLabels = new Mat(2, 1, CvType.CV_32SC1)
    val InitialImages = new java.util.ArrayList[Mat]()

    for (idx <- 0 to 1) {
      val bytes = getClass.getResourceAsStream(s"/train/$idx.png").readAllBytes()
      val image: Mat = matFromBytes(bytes)
      initialLabels.put(idx, 0, idx)
      InitialImages.add(image)
    }

    recognizer.train(InitialImages, initialLabels)
  }

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case AddFace(face, personLabel, replyTo) =>
        val labels = new Mat(1, 1, CvType.CV_32SC1)
        val images = new util.ArrayList[Mat]()
        labels.put(0, 0, personLabel)
        images.add(face.alignedImageGsMat)
        recognizer.update(images, labels)
        replyTo ! AltitudeActorSystem.EmptyResponse()
        Behaviors.same

      case Initialize(replyTo) =>
        initialize()
        replyTo ! AltitudeActorSystem.EmptyResponse()
        Behaviors.same
    }
  }
}
