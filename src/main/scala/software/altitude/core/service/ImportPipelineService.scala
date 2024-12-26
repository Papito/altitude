package software.altitude.core.service

import org.apache.pekko.Done
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.{Actor, Props}
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import org.apache.pekko.stream.OverflowStrategy
import org.apache.pekko.stream.QueueOfferResult
import org.apache.pekko.stream.scaladsl.{Flow, Keep, Sink, Source, SourceQueueWithComplete}
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.altitude.core.Altitude
import software.altitude.core.AltitudeActorSystem
import software.altitude.core.pipeline.flows.{AddPreviewFlow, AssignIdFlow, CheckDuplicateFlow, CheckMetadataFlow, ExtractMetadataFlow, FacialRecognitionFlow, FileStoreFlow, PersistAndIndexAssetFlow}
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
  private val persistAndIndexFlow = PersistAndIndexAssetFlow(app)
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
      .via(persistAndIndexFlow)
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

  private def runAsQueue() = {
    logger.info("Starting the import queue pipeline")
    val bufferSize = parallelism * 2

    val (actorRef, source) = Source.actorRef[TDataAssetWithContext](
      completionMatcher = PartialFunction.empty,
      failureMatcher = PartialFunction.empty,
      bufferSize = parallelism * 1000,
      overflowStrategy = OverflowStrategy.fail
    ).preMaterialize()

    source
      .via(combinedFlow)
      .toMat(Sink.ignore)(Keep.left)
      .run()

    actorRef
  }

  def addToQueue(asset: TDataAssetWithContext): Future[Unit] = {
    Future {
      queueImportPipeline ! asset
    }(system.executionContext)
  }
  def shutdown(): Unit = {
    system.terminate()
    logger.warn("Actor system terminated")
  }
}
