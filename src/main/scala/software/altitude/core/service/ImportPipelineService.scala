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
import software.altitude.core.RequestContext
import software.altitude.core.dao.jdbc.BaseDao
import software.altitude.core.models.Asset
import software.altitude.core.models.AssetWithData
import software.altitude.core.models.Repository
import software.altitude.core.models.User

import scala.concurrent.Future

case class PipelineContext(repository: Repository, account: User)

class ImportPipelineService(app: Altitude) {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "pipeline-system")
  private val parallelism = Runtime.getRuntime.availableProcessors

  private def setThreadLocalRequestContext(ctx: PipelineContext): Unit = {
    RequestContext.repository.value = Some(ctx.repository)
    RequestContext.account.value = Some(ctx.account)
  }

  private def threadInfo = s"(${Thread.currentThread().getName})"

  private val assignIdFlow: Flow[(AssetWithData, PipelineContext), (AssetWithData, PipelineContext), NotUsed] =
    Flow[(AssetWithData, PipelineContext)].map {
      case (dataAsset, ctx) =>
        val asset: Asset = dataAsset.asset.copy(
          id = Some(BaseDao.genId)
        )
        println(s"$threadInfo Assigning ID to asset: ${asset.id.get}")

        (AssetWithData(asset, dataAsset.data), ctx)
    }

  private val extractMetadataFlow: Flow[(AssetWithData, PipelineContext), (AssetWithData, PipelineContext), NotUsed] =
    Flow[(AssetWithData, PipelineContext)].mapAsync(parallelism) {
      case (dataAsset, ctx) =>
        println(s"$threadInfo Extracting metadata for asset ${dataAsset.asset.persistedId}")
        val extractedMetadata = app.service.metadataExtractor.extract(dataAsset.data)
        val publicMetadata = Asset.getPublicMetadata(extractedMetadata)

        val asset: Asset = dataAsset.asset.copy(
          extractedMetadata = extractedMetadata,
          publicMetadata = publicMetadata
        )

        Future.successful(AssetWithData(asset, dataAsset.data), ctx)
    }

  private val fileStoreFlow: Flow[(AssetWithData, PipelineContext), (AssetWithData, PipelineContext), NotUsed] =
    Flow[(AssetWithData, PipelineContext)].mapAsync(parallelism) {
      case (dataAsset, ctx) =>
        setThreadLocalRequestContext(ctx)

        println(s"$threadInfo Storing asset ${dataAsset.asset.persistedId} in file store")
        app.service.fileStore.addAsset(dataAsset)
        Future.successful(dataAsset, ctx)
    }

  private val persistAssetFlow: Flow[(AssetWithData, PipelineContext), (AssetWithData, PipelineContext), NotUsed] =
    Flow[(AssetWithData, PipelineContext)].mapAsync(parallelism) {
      case (dataAsset, ctx) =>
        setThreadLocalRequestContext(ctx)

        println(s"$threadInfo Persisting asset ${dataAsset.asset.persistedId}")
        app.service.asset.add(dataAsset.asset)
        Future.successful(dataAsset, ctx)
    }

  private val indexAssetFlow: Flow[(AssetWithData, PipelineContext), (AssetWithData, PipelineContext), NotUsed] =
    Flow[(AssetWithData, PipelineContext)].map {
      case (dataAsset, ctx) =>
        setThreadLocalRequestContext(ctx)

        println(s"$threadInfo Indexing asset  ${dataAsset.asset.persistedId}")
        app.service.search.indexAsset(dataAsset.asset)
        (dataAsset, ctx)
    }

  private val facialRecognitionFlow: Flow[(AssetWithData, PipelineContext), (AssetWithData, PipelineContext), NotUsed] =
    Flow[(AssetWithData, PipelineContext)].map {
      case (dataAsset, ctx) =>
        setThreadLocalRequestContext(ctx)

        println(s"$threadInfo Running facial recognition ${dataAsset.asset.persistedId}")
        app.service.faceRecognition.processAsset(dataAsset)
        (dataAsset, ctx)
    }

  private val updateStatsFlow: Flow[(AssetWithData, PipelineContext), (AssetWithData, PipelineContext), NotUsed] =
    Flow[(AssetWithData, PipelineContext)].mapAsync(parallelism) {
      case (dataAsset, ctx) =>
        setThreadLocalRequestContext(ctx)

        println(s"$threadInfo Updating stats for ${dataAsset.asset.persistedId}")
        app.service.stats.addAsset(dataAsset.asset)
        Future.successful(dataAsset, ctx)
    }

  private val addPreviewFlow: Flow[(AssetWithData, PipelineContext), (AssetWithData, PipelineContext), NotUsed] =
    Flow[(AssetWithData, PipelineContext)].mapAsync(parallelism) {
      case (dataAsset, ctx) =>
        setThreadLocalRequestContext(ctx)

        println(s"$threadInfo Generating preview ${dataAsset.asset.id.get}")
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
      .via(assignIdFlow)
      .async
      .via(parallelGraph)
      .async
      .via(persistAssetFlow)
      // .via(facialRecognitionFlow)
      .via(indexAssetFlow)
      .map { case (dataAsset, _) => dataAsset }
      .runWith(Sink.seq)
  }
  def shutdown(): Unit = {
    system.terminate()
  }
}
