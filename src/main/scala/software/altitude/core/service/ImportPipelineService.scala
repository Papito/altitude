package software.altitude.core.service

import org.apache.pekko.Done
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.stream.OverflowStrategy
import org.apache.pekko.stream.QueueOfferResult
import org.apache.pekko.stream.scaladsl.{Flow, Keep, Sink, Source, SourceQueueWithComplete}
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
import software.altitude.core.pipeline.PipelineTypes.TAssetOrInvalidWithContext
import software.altitude.core.pipeline.PipelineTypes.TDataAssetWithContext
import software.altitude.core.pipeline.actors.StripBinaryDataFlow
import software.altitude.core.pipeline.sinks.{ErrorLoggingSink, WsNotificationSink}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

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
  private val stripBinaryDataFlow = StripBinaryDataFlow(app)
  private val wsNotificationSink = WsNotificationSink(app)
  private val errorLoggingSink = ErrorLoggingSink()

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
      .via(stripBinaryDataFlow)
      .alsoTo(wsNotificationSink)
      .alsoTo(errorLoggingSink)

  private val queueImportPipeline = runAsQueue()

  def run(
           source: Source[TDataAssetWithContext, NotUsed],
           outputSink: Sink[TAssetOrInvalidWithContext, Future[Seq[TAssetOrInvalidWithContext]]]): Future[Seq[TAssetOrInvalidWithContext]] = {

    source
      .via(combinedFlow)
      .runWith(outputSink)
  }

  private def runAsQueue(): SourceQueueWithComplete[TDataAssetWithContext] = {
    logger.info("Starting the import queue pipeline")
    val bufferSize = parallelism * 2
    val queueSource = Source.queue[TDataAssetWithContext](bufferSize, OverflowStrategy.backpressure)

    val queue = queueSource
      .merge(Source.never)
      .via(combinedFlow)
      .toMat(Sink.foreach(n => println(s"Completed processing: $n")))(Keep.left)
      .run()

    queue.watchCompletion().onComplete {
      case Success(_) =>
        logger.warn("Import queue pipeline completed successfully")
      case Failure(exception) =>
        logger.warn(s"Import queue pipeline failed with exception: $exception")
    }(system.executionContext)

    queue
  }

  def addToQueue(asset: TDataAssetWithContext): Future[QueueOfferResult] = {
    queueImportPipeline.offer(asset)
  }

  def shutdown(): Unit = {
    queueImportPipeline.complete()
    val queueCompletedFut = queueImportPipeline.watchCompletion()
    Await.result(queueCompletedFut, 10.seconds)
    system.terminate()
    logger.warn("Actor system terminated")
  }
}
