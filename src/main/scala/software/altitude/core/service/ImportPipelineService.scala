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

import scala.concurrent.Future

import software.altitude.core.Altitude
import software.altitude.core.RequestContext
import software.altitude.core.dao.jdbc.BaseDao
import software.altitude.core.models.Asset
import software.altitude.core.models.AssetWithData
import software.altitude.core.models.Repository
import software.altitude.core.models.User

case class PipelineContext(repository: Repository,
                           account: User)

object ImportPipelineService {

  /**
   * Set to "true" to enable debugging output to console.
   *
   * Useful for understanding the flow of the pipeline and its use of threads.
   */
  private val DEBUG = false
}

case class Valid(asset: AssetWithData)

case class Invalid(payload: AssetWithData, cause: Option[Throwable])

class ImportPipelineService(app: Altitude) {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "pipeline-system")
  private val parallelism = Runtime.getRuntime.availableProcessors

  private def setThreadLocalRequestContext(ctx: PipelineContext): Unit = {
    RequestContext.repository.value = Some(ctx.repository)
    RequestContext.account.value = Some(ctx.account)
  }

  private def threadInfo(msg: String) = {
    if (ImportPipelineService.DEBUG) {
      println(s"(${Thread.currentThread().getName}) $msg")
    } else {
      ""
    }
  }

  private val checkMediaTypeFlow: Flow[(AssetWithData, PipelineContext), (AssetWithData, PipelineContext), NotUsed] =
    Flow[(AssetWithData, PipelineContext)].map {
      case (dataAsset, ctx) =>
        threadInfo(s"Checking media type: ${dataAsset.asset.fileName}: ${dataAsset.asset.assetType.toJson}")
        app.service.library.checkMediaType(dataAsset.asset)
        (dataAsset, ctx)
    }

  private val assignIdFlow: Flow[(AssetWithData, PipelineContext), (AssetWithData, PipelineContext), NotUsed] =
    Flow[(AssetWithData, PipelineContext)].map {
      case (dataAsset, ctx) =>
        val asset: Asset = dataAsset.asset.copy(
          id = Some(BaseDao.genId)
        )
        threadInfo(s"Assigning ID to asset: ${asset.id.get}")

        (AssetWithData(asset, dataAsset.data), ctx)
    }

  private val extractMetadataFlow: Flow[(AssetWithData, PipelineContext), (AssetWithData, PipelineContext), NotUsed] =
    Flow[(AssetWithData, PipelineContext)].mapAsync(parallelism) {
      case (dataAsset, ctx) =>
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

        Future.successful(AssetWithData(asset, dataAsset.data), ctx)
    }

  private val fileStoreFlow: Flow[(AssetWithData, PipelineContext), (AssetWithData, PipelineContext), NotUsed] =
    Flow[(AssetWithData, PipelineContext)].mapAsync(parallelism) {
      case (dataAsset, ctx) =>
        setThreadLocalRequestContext(ctx)

        threadInfo(s"\tStoring asset ${dataAsset.asset.persistedId} in file store")
        app.service.fileStore.addAsset(dataAsset)
        Future.successful(dataAsset, ctx)
    }

  private val persistAndIndexAssetFlow: Flow[(AssetWithData, PipelineContext), (AssetWithData, PipelineContext), NotUsed] =
    Flow[(AssetWithData, PipelineContext)].mapAsync(parallelism) {
      case (dataAsset, ctx) =>
        setThreadLocalRequestContext(ctx)

        app.txManager.withTransaction {
          threadInfo(s"\tPersisting asset ${dataAsset.asset.persistedId}")
          app.service.asset.add(dataAsset.asset)
          threadInfo(s"\tIndexing asset ${dataAsset.asset.persistedId}")
          app.service.search.indexAsset(dataAsset.asset)
        }
        Future.successful(dataAsset, ctx)
    }

  private val indexAssetFlow: Flow[(AssetWithData, PipelineContext), (AssetWithData, PipelineContext), NotUsed] =
    Flow[(AssetWithData, PipelineContext)].map {
      case (dataAsset, ctx) =>
        setThreadLocalRequestContext(ctx)

        threadInfo(s"\tIndexing asset ${dataAsset.asset.persistedId}")
        app.service.search.indexAsset(dataAsset.asset)
        (dataAsset, ctx)
    }

  private val facialRecognitionFlow: Flow[(AssetWithData, PipelineContext), (AssetWithData, PipelineContext), NotUsed] =
    Flow[(AssetWithData, PipelineContext)].map {
      case (dataAsset, ctx) =>
        setThreadLocalRequestContext(ctx)

        threadInfo(s"\tRunning facial recognition ${dataAsset.asset.persistedId}")
        app.service.faceRecognition.processAsset(dataAsset)
        (dataAsset, ctx)
    }

  private val updateStatsFlow: Flow[(AssetWithData, PipelineContext), (AssetWithData, PipelineContext), NotUsed] =
    Flow[(AssetWithData, PipelineContext)].mapAsync(parallelism) {
      case (dataAsset, ctx) =>
        setThreadLocalRequestContext(ctx)

        threadInfo(s"\tUpdating stats for ${dataAsset.asset.persistedId}")
        app.service.stats.addAsset(dataAsset.asset)
        Future.successful(dataAsset, ctx)
    }

  private val addPreviewFlow: Flow[(AssetWithData, PipelineContext), (AssetWithData, PipelineContext), NotUsed] =
    Flow[(AssetWithData, PipelineContext)].mapAsync(parallelism) {
      case (dataAsset, ctx) =>
        setThreadLocalRequestContext(ctx)

        threadInfo(s"\tGenerating preview ${dataAsset.asset.persistedId}")
        app.service.library.addPreview(dataAsset)
        Future.successful(dataAsset, ctx)
    }

  def run(source: Source[(AssetWithData, PipelineContext), NotUsed]): Future[Seq[AssetWithData]] = {

    val parallelGraph: Graph[FlowShape[(AssetWithData, PipelineContext), (AssetWithData, PipelineContext)], NotUsed] = GraphDSL.create() {
      implicit builder =>
        import GraphDSL.Implicits._

        val broadcast = builder.add(Broadcast[(AssetWithData, PipelineContext)](4))

        val zip = builder.add(
          ZipWith[
            (AssetWithData, PipelineContext),
            (AssetWithData, PipelineContext),
            (AssetWithData, PipelineContext),
            (AssetWithData, PipelineContext),
            (AssetWithData, PipelineContext)]((extractMetadataSubstream, _, _, _) => extractMetadataSubstream)
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
      .via(persistAndIndexAssetFlow)
      .async
      .via(facialRecognitionFlow)
      .map { case (dataAsset, _) => dataAsset }
      .runWith(Sink.seq)
  }
  def shutdown(): Unit = {
    system.terminate()
  }
}
