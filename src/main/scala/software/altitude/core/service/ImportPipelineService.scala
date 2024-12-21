package software.altitude.core.service

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.scaladsl.Source
import software.altitude.core.Altitude
import software.altitude.core.models.AssetWithData
import software.altitude.core.pipeline.AssignIdFlow
import software.altitude.core.pipeline.CheckMetadataFlow
import software.altitude.core.pipeline.FacialRecognitionFlow
import software.altitude.core.pipeline.ParallelFlowsGraph
import software.altitude.core.pipeline.PersistAndIndexAssetFlow
import software.altitude.core.pipeline.PipelineTypes.TAssetOrInvalidWithContext
import software.altitude.core.pipeline.PipelineTypes.TAssetWithContext
import software.altitude.core.pipeline.Sinks.defaultErrorSink

import scala.concurrent.Future

class ImportPipelineService(app: Altitude) {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "pipeline-system")

  private val checkMediaTypeFlow = CheckMetadataFlow(app)
  private val assignIdFlow = AssignIdFlow(app)
  private val persistAndIndexAssetFlow = PersistAndIndexAssetFlow(app)
  private val facialRecognitionFlow = FacialRecognitionFlow(app)
  private val parallelFlowsGraph = ParallelFlowsGraph(app)

  def run(source: Source[TAssetWithContext, NotUsed]): Future[Seq[AssetWithData]] = {
    source
      .via(checkMediaTypeFlow)
      .via(assignIdFlow)
      .async
      .via(parallelFlowsGraph)
      .via(persistAndIndexAssetFlow)
      .async
      .via(facialRecognitionFlow)
      .alsoTo(Flow[TAssetOrInvalidWithContext].collect {
        case (Right(invalid), _) => invalid }.to(
        defaultErrorSink))
      .collect {
        case (Left(dataAsset), _) => dataAsset }
      .runWith(Sink.seq)
  }

  def shutdown(): Unit = {
    system.terminate()
  }
}
