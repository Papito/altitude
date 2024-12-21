package software.altitude.core.service

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.stream.FlowShape
import org.apache.pekko.stream.Graph
import org.apache.pekko.stream.scaladsl.Broadcast
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.stream.scaladsl.GraphDSL
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.scaladsl.ZipWith
import software.altitude.core.Altitude
import software.altitude.core.DuplicateException
import software.altitude.core.RequestContext
import software.altitude.core.UnsupportedMediaTypeException
import software.altitude.core.dao.jdbc.BaseDao
import software.altitude.core.models.Asset
import software.altitude.core.models.AssetWithData
import software.altitude.core.models.Repository
import software.altitude.core.models.User

import scala.concurrent.Future

case class PipelineContext(repository: Repository, account: User)

object ImportPipelineService {

  /**
   * Set to "true" to enable debugging output to console.
   *
   * Useful for understanding the flow of the pipeline and its use of threads.
   */
  private val DEBUG = false
}

case class Invalid(payload: AssetWithData, cause: Option[Throwable])

class ImportPipelineService(app: Altitude) {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "pipeline-system")
  private val parallelism = Runtime.getRuntime.availableProcessors
  private type TAssetOrInvalid = Either[AssetWithData, Invalid]
  private type TAssetOrInvalidWithContext = (TAssetOrInvalid, PipelineContext)
  private type TAssetWithContext = (AssetWithData, PipelineContext)

  private def setThreadLocalRequestContext(ctx: PipelineContext): Unit = {
    RequestContext.repository.value = Some(ctx.repository)
    RequestContext.account.value = Some(ctx.account)
  }

  private def threadInfo(msg: String): Unit = {
    if (ImportPipelineService.DEBUG) {
      println(s"(${Thread.currentThread().getName}) $msg")
    }
  }

  // Modeled after the discussion and ScalaDays talk here:
  // https://discuss.akka.io/t/akka-streams-exception-handling/6557
  private val defaultErrorSink = Flow[Invalid]
    .map(
      each => {
        if (ImportPipelineService.DEBUG) println(s"Reached errorSink: $each")
      })
    .to(Sink.ignore)

  private val checkMediaTypeFlow: Flow[TAssetWithContext, TAssetOrInvalidWithContext, NotUsed] =
    Flow[(AssetWithData, PipelineContext)].map {
      case (dataAsset, ctx) =>
        threadInfo(s"Checking media type: ${dataAsset.asset.fileName}: ${dataAsset.asset.assetType.toJson}")

        try {
          app.service.library.checkMediaType(dataAsset.asset)
          (Left(dataAsset), ctx)
        } catch {
          case e: UnsupportedMediaTypeException =>
            (Right(Invalid(dataAsset, Some(e))), ctx)
        }
    }
  private val assignIdFlow: Flow[TAssetOrInvalidWithContext, TAssetOrInvalidWithContext, NotUsed] =
    Flow[TAssetOrInvalidWithContext].map {
      case (Left(dataAsset), ctx) =>
        val asset: Asset = dataAsset.asset.copy(
          id = Some(BaseDao.genId)
        )
        threadInfo(s"Assigning ID to asset: ${asset.id.get}")

        (Left(dataAsset.copy(asset = asset)), ctx)
      case (Right(invalid), ctx) => (Right(invalid), ctx)
    }

  private val extractMetadataFlow: Flow[TAssetOrInvalidWithContext, TAssetOrInvalidWithContext, NotUsed] =
    Flow[TAssetOrInvalidWithContext].mapAsync(parallelism) {
      case (Left(dataAsset), ctx) =>
        setThreadLocalRequestContext(ctx)

        threadInfo(s"\tExtracting metadata for asset: ${dataAsset.asset.persistedId}")
        val userMetadata = app.service.metadata.cleanAndValidate(dataAsset.asset.userMetadata)
        val extractedMetadata = app.service.metadataExtractor.extract(dataAsset.data)
        val publicMetadata = Asset.getPublicMetadata(extractedMetadata)

        val asset: Asset = dataAsset.asset.copy(
          extractedMetadata = extractedMetadata,
          publicMetadata = publicMetadata,
          userMetadata = userMetadata
        )

        Future.successful((Left(dataAsset.copy(asset = asset)), ctx))
      case (Right(invalid), ctx) => Future.successful((Right(invalid), ctx))
    }

  private val fileStoreFlow: Flow[TAssetOrInvalidWithContext, TAssetOrInvalidWithContext, NotUsed] =
    Flow[TAssetOrInvalidWithContext].mapAsync(parallelism) {
      case (Left(dataAsset), ctx) =>
        setThreadLocalRequestContext(ctx)

        threadInfo(s"\tStoring asset ${dataAsset.asset.persistedId} in file store")
        app.service.fileStore.addAsset(dataAsset)
        Future.successful((Left(dataAsset), ctx))
      case (Right(invalid), ctx) => Future.successful((Right(invalid), ctx))
    }

  private val persistAndIndexAssetFlow: Flow[TAssetOrInvalidWithContext, TAssetOrInvalidWithContext, NotUsed] =
    Flow[TAssetOrInvalidWithContext].mapAsync(parallelism) {
      case (Left(dataAsset), ctx) =>
        setThreadLocalRequestContext(ctx)

        app.txManager.withTransaction {
          try {
            threadInfo(s"\tPersisting asset ${dataAsset.asset.persistedId}")
            app.service.asset.add(dataAsset.asset)
            threadInfo(s"\tIndexing asset ${dataAsset.asset.persistedId}")
            app.service.search.indexAsset(dataAsset.asset)
            Future.successful((Left(dataAsset), ctx))
          } catch {
            case e: DuplicateException =>
              Future.successful(Right(Invalid(dataAsset, Some(e))), ctx)
          }
        }
      case (Right(invalid), ctx) =>
        Future.successful((Right(invalid), ctx))
    }

  private val facialRecognitionFlow: Flow[TAssetOrInvalidWithContext, TAssetOrInvalidWithContext, NotUsed] =
    Flow[TAssetOrInvalidWithContext].map {
      case (Left(dataAsset), ctx) =>
        setThreadLocalRequestContext(ctx)

        threadInfo(s"\tRunning facial recognition ${dataAsset.asset.persistedId}")
        app.service.faceRecognition.processAsset(dataAsset)
        (Left(dataAsset), ctx)
      case (Right(invalid), ctx) => (Right(invalid), ctx)
    }

  private val updateStatsFlow: Flow[TAssetOrInvalidWithContext, TAssetOrInvalidWithContext, NotUsed] =
    Flow[TAssetOrInvalidWithContext].mapAsync(parallelism) {
      case (Left(dataAsset), ctx) =>
        setThreadLocalRequestContext(ctx)

        threadInfo(s"\tUpdating stats for ${dataAsset.asset.persistedId}")
        app.service.stats.addAsset(dataAsset.asset)
        Future.successful((Left(dataAsset), ctx))
      case (Right(invalid), ctx) => Future.successful((Right(invalid), ctx))
    }

  private val addPreviewFlow: Flow[TAssetOrInvalidWithContext, TAssetOrInvalidWithContext, NotUsed] =
    Flow[TAssetOrInvalidWithContext].mapAsync(parallelism) {
      case (Left(dataAsset), ctx) =>
        setThreadLocalRequestContext(ctx)

        threadInfo(s"\tGenerating preview ${dataAsset.asset.persistedId}")
        app.service.library.addPreview(dataAsset)
        Future.successful((Left(dataAsset), ctx))
      case (Right(invalid), ctx) => Future.successful((Right(invalid), ctx))
    }

  def run(source: Source[TAssetWithContext, NotUsed]): Future[Seq[AssetWithData]] = {
    val parallelGraph: Graph[FlowShape[TAssetOrInvalidWithContext, TAssetOrInvalidWithContext], NotUsed] = GraphDSL.create() {
      implicit builder =>
        import GraphDSL.Implicits._

        val broadcast = builder.add(Broadcast[TAssetOrInvalidWithContext](4))

        val zip = builder.add(
          ZipWith[TAssetOrInvalidWithContext, TAssetOrInvalidWithContext, TAssetOrInvalidWithContext, TAssetOrInvalidWithContext, TAssetOrInvalidWithContext](
            (extractMetadataSubstream, _, _, _) => extractMetadataSubstream)
        )

        // Define the flows
        val extractMetadataFlowShape = builder.add(extractMetadataFlow)
        val fileStoreFlowShape = builder.add(fileStoreFlow)
        val updateStatsFlowShape = builder.add(updateStatsFlow)
        val addPreviewFlowShape = builder.add(addPreviewFlow)

        // Connect the flows
        broadcast ~> extractMetadataFlowShape ~> zip.in0
        broadcast ~> fileStoreFlowShape ~> zip.in1
        broadcast ~> updateStatsFlowShape ~> zip.in2
        broadcast ~> addPreviewFlowShape ~> zip.in3

        FlowShape(broadcast.in, zip.out)
    }

    source
      .via(checkMediaTypeFlow)
      .via(assignIdFlow)
      .async
      .via(parallelGraph)
      // Yes, we are creating artifacts before persisting the asset
      // and this can fail, but we need the metadata for this step.
      // The dangling orphan data can/should be cleaned up in the
      // terminal Sink
      .via(persistAndIndexAssetFlow)
      .async
      .via(facialRecognitionFlow)
      .alsoTo(Flow[TAssetOrInvalidWithContext].collect { case (Right(invalid), _) => invalid }.to(defaultErrorSink))
      .collect { case (Left(dataAsset), _) => dataAsset }
      .runWith(Sink.seq)
  }

  def shutdown(): Unit = {
    system.terminate()
  }
}
