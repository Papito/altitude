package software.altitude.core.service

import org.apache.pekko.Done
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.stream.OverflowStrategy
import org.apache.pekko.stream.QueueOfferResult
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.stream.scaladsl.Keep
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.scaladsl.SourceQueueWithComplete
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.altitude.core.Altitude
import software.altitude.core.AltitudeActorSystem
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
import software.altitude.core.pipeline.PipelineTypes.TAssetOrInvalidWithContext
import software.altitude.core.pipeline.PipelineTypes.TDataAssetWithContext
import software.altitude.core.pipeline.Sinks.voidErrorSink
import software.altitude.core.pipeline.actors.ImportStatusWsActor

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class ImportPipelineService(app: Altitude) {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  implicit val system: ActorSystem[AltitudeActorSystem.Command] = app.actorSystem

  private val checkMediaTypeFlow = CheckMetadataFlow(app)
  private val assignIdFlow = AssignIdFlow(app)
  private val persistAndIndexAssetFlow = PersistAndIndexAssetFlow(app)
  private val facialRecognitionFlow = FacialRecognitionFlow(app)
  private val extractMetadataFlow = ExtractMetadataFlow(app)
  private val fileStoreFlow = FileStoreFlow(app)
  private val addPreviewFlow = AddPreviewFlow(app)
  private val checkDuplicateFlow = CheckDuplicateFlow(app)

  private val combinedFlow: Flow[TDataAssetWithContext, TAssetOrInvalidWithContext, NotUsed] =
    Flow[TDataAssetWithContext]
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
        // strip binary data from the asset, leaving just the Asset metadata and pipeline context
        case (assetWithDataOrInvalid, ctx) =>
          val assetOrInvalid = assetWithDataOrInvalid match {
            case Left(assetWithData) => Left(assetWithData.asset)
            case Right(invalid) => Right(invalid)
          }

          (assetOrInvalid, ctx)
      }
      .map {
        case (assetOrInvalid, ctx) =>
          app.actorSystem ! ImportStatusWsActor.UserWideImportStatus(ctx.account.persistedId, assetOrInvalid)
          (assetOrInvalid, ctx)
      }

  private val (queueImportPipeline, queuePipelineCompletedFut) = runAsQueue()

  def run(
      source: Source[TDataAssetWithContext, NotUsed],
      outputSink: Sink[TAssetOrInvalidWithContext, Future[Seq[TAssetOrInvalidWithContext]]],
      errorSink: Sink[Invalid, Future[Done]] = voidErrorSink): Future[Seq[TAssetOrInvalidWithContext]] = {

    source
      .via(combinedFlow)
      .alsoTo(Sink.foreach {
        case (assetOrInvalid, ctx) =>
          assetOrInvalid match {
            case Left(asset) => (Left(asset), ctx)
            case Right(invalid) => errorSink.runWith(Source.single(invalid))
          }
      })
      .runWith(outputSink)
  }

  private def runAsQueue(): (SourceQueueWithComplete[TDataAssetWithContext], Future[Done]) = {
    logger.info("Starting the import queue pipeline")
    val bufferSize = parallelism * 4 // Reckless guess on my part of what should be a good buffer size
    val queueSource = Source.queue[TDataAssetWithContext](bufferSize, OverflowStrategy.backpressure)

    val (queue, completedFut) = queueSource
      .via(combinedFlow)
      .toMat(Sink.ignore)(Keep.both)
      .run()

    (queue, completedFut)
  }

  def addToQueue(asset: TDataAssetWithContext): Future[QueueOfferResult] = {
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
