package software.altitude.core.service

import org.apache.pekko.Done
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.scaladsl.Source
import software.altitude.core.Altitude
import software.altitude.core.pipeline.{AssignIdFlow, CheckMetadataFlow, ExtractMetadataFlow, FacialRecognitionFlow, ParallelFlowsGraph, PersistAndIndexAssetFlow}
import software.altitude.core.pipeline.PipelineTypes.Invalid
import software.altitude.core.pipeline.PipelineTypes.TAssetOrInvalid
import software.altitude.core.pipeline.PipelineTypes.TAssetWithContext
import software.altitude.core.pipeline.Sinks.voidErrorSink

import scala.concurrent.Future

class ImportPipelineService(app: Altitude) {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "pipeline-system")

  private val checkMediaTypeFlow = CheckMetadataFlow(app)
  private val assignIdFlow = AssignIdFlow(app)
  private val persistAndIndexAssetFlow = PersistAndIndexAssetFlow(app)
  private val facialRecognitionFlow = FacialRecognitionFlow(app)
  private val parallelFlowsGraph = ParallelFlowsGraph(app)
  private val extractMetadataFlow = ExtractMetadataFlow(app)


  def run(
      source: Source[TAssetWithContext, NotUsed],
      outputSink: Sink[TAssetOrInvalid, Future[Seq[TAssetOrInvalid]]],
      errorSink: Sink[Invalid, Future[Done]] = voidErrorSink): Future[Seq[TAssetOrInvalid]] = {
    source
      .via(checkMediaTypeFlow)
      .via(assignIdFlow)
      .async
      .via(extractMetadataFlow)
      .async
      .via(persistAndIndexAssetFlow)
      .async
      .via(parallelFlowsGraph)
      .async
      .via(facialRecognitionFlow)
      .alsoTo(Sink.foreach {
        case (Right(invalid), _) => errorSink.runWith(Source.single(invalid))
        case _ =>
      })
      .map {
        // strip pipeline context
        case (assetWithDataOrInvalid, _) => assetWithDataOrInvalid
      }
      .map {
        case Left(assetWithData) => Left(assetWithData.asset)
        case Right(invalid) => Right(invalid)
      }
      .runWith(outputSink)
  }

  def shutdown(): Unit = {
    system.terminate()
  }
}
