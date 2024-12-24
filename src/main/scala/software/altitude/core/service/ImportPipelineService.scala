package software.altitude.core.service

import org.apache.pekko.Done
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.stream.{OverflowStrategy, QueueOfferResult}
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.stream.scaladsl.Keep
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.scaladsl.SourceQueueWithComplete
import org.slf4j.{Logger, LoggerFactory}
import software.altitude.core.Altitude
import software.altitude.core.pipeline.AddPreviewFlow
import software.altitude.core.pipeline.AssignIdFlow
import software.altitude.core.pipeline.CheckDuplicateFlow
import software.altitude.core.pipeline.CheckMetadataFlow
import software.altitude.core.pipeline.ExtractMetadataFlow
import software.altitude.core.pipeline.FacialRecognitionFlow
import software.altitude.core.pipeline.FileStoreFlow
import software.altitude.core.pipeline.PersistAndIndexAssetFlow
import software.altitude.core.pipeline.PipelineConstants.parallelism
import software.altitude.core.pipeline.PipelineTypes.Invalid
import software.altitude.core.pipeline.PipelineTypes.TAssetOrInvalid
import software.altitude.core.pipeline.PipelineTypes.TAssetWithContext
import software.altitude.core.pipeline.Sinks.voidErrorSink

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, Future}

class ImportPipelineService(app: Altitude) {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "pipeline-system")

  private val checkMediaTypeFlow = CheckMetadataFlow(app)
  private val assignIdFlow = AssignIdFlow(app)
  private val persistAndIndexAssetFlow = PersistAndIndexAssetFlow(app)
  private val facialRecognitionFlow = FacialRecognitionFlow(app)
  private val extractMetadataFlow = ExtractMetadataFlow(app)
  private val fileStoreFlow = FileStoreFlow(app)
  private val addPreviewFlow = AddPreviewFlow(app)
  private val checkDuplicateFlow = CheckDuplicateFlow(app)

  private val combinedFlow: Flow[TAssetWithContext, TAssetOrInvalid, NotUsed] =
    Flow[TAssetWithContext]
      .via(checkMediaTypeFlow)
      .via(checkDuplicateFlow)
      .via(assignIdFlow)
      .via(extractMetadataFlow)
      .via(persistAndIndexAssetFlow)
      .async
      .via(facialRecognitionFlow)
      .async
      .via(fileStoreFlow)
      .async
      .via(addPreviewFlow)
      .map {
        // strip pipeline context
        case (assetWithDataOrInvalid, _) => assetWithDataOrInvalid
      }
      .map {
        // strip binary data from the asset, leaving just the Asset (metadata)
        case Left(assetWithData) => Left(assetWithData.asset)
        case Right(invalid) => Right(invalid)
      }

  private val (queueImportPipeline, queuePipelineCompletedFut) = runAsQueue()

  def run(
      source: Source[TAssetWithContext, NotUsed],
      outputSink: Sink[TAssetOrInvalid, Future[Seq[TAssetOrInvalid]]],
      errorSink: Sink[Invalid, Future[Done]] = voidErrorSink): Future[Seq[TAssetOrInvalid]] = {

    source
      .via(combinedFlow)
      .alsoTo(Sink.foreach {
        case Right(invalid) => errorSink.runWith(Source.single(invalid))
        case _ =>
      })
      .runWith(outputSink)
  }

  private def runAsQueue(): (SourceQueueWithComplete[TAssetWithContext], Future[Done]) = {
    logger.info("Starting the import pipeline")
    val queueSource = Source.queue[TAssetWithContext](parallelism * 3, OverflowStrategy.backpressure)

    val (queue, completedFut) = queueSource
      .via(combinedFlow)
      .toMat(Sink.ignore)(Keep.both)
      .run()

    (queue, completedFut)
  }

  def addToQueue(asset: TAssetWithContext): Future[QueueOfferResult] = {
      queueImportPipeline.offer(asset)
  }

  def shutdown(): Unit = {
    queueImportPipeline.complete()
    logger.warn("Waiting for the import pipeline to complete")
    Await.result(queuePipelineCompletedFut, 15.seconds)
    logger.warn("Import pipeline completed")
    system.terminate()
    logger.warn("Actor system terminated")
  }
}
