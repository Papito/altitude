package software.altitude.core.service

import org.apache.pekko.Done
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.scaladsl.Source
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.altitude.core.Altitude
import software.altitude.core.AltitudeActorSystem
import software.altitude.core.models.FaceForTraining
import software.altitude.core.pipeline.flows._

import scala.concurrent.Future

class FaceRecLoadPipelineService(app: Altitude) {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  implicit val system: ActorSystem[AltitudeActorSystem.Command] = app.actorSystem

  private val readFaceDataFlow = ReadFaceDataFlow(app)

  def run(source: Source[FaceForTraining, NotUsed]): Future[Done] = {
    source
      .via(readFaceDataFlow)
      .runWith(Sink.ignore)
  }

}
