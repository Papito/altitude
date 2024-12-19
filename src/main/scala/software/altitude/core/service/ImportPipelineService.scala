package software.altitude.core.service

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.scaladsl.Source
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

  private val extractMetadataFlow: Flow[(AssetWithData, PipelineContext), (AssetWithData, PipelineContext), NotUsed] =
    Flow[(AssetWithData, PipelineContext)].mapAsync(parallelism) {
      case (dataAsset, ctx) =>
        val assetId = BaseDao.genId
        val extractedMetadata = app.service.metadataExtractor.extract(dataAsset.data)
        val publicMetadata = Asset.getPublicMetadata(extractedMetadata)

        val asset: Asset = dataAsset.asset.copy(
          id = Some(assetId),
          extractedMetadata = extractedMetadata,
          publicMetadata = publicMetadata
        )

        println(s"(Thread: ${Thread.currentThread().getName})")
        Future.successful(AssetWithData(asset, dataAsset.data), ctx)
    }

  private val fileStoreFlow: Flow[(AssetWithData, PipelineContext), (AssetWithData, PipelineContext), NotUsed] =
    Flow[(AssetWithData, PipelineContext)].mapAsync(parallelism) {
      case (dataAsset, ctx) =>
        RequestContext.repository.value = Some(ctx.repository)
        RequestContext.account.value = Some(ctx.account)
        println(s"(Thread: ${Thread.currentThread().getName})")
        println(s"Storing asset ${dataAsset.asset.id.get} in file store")
        app.service.fileStore.addAsset(dataAsset)
        Future.successful(dataAsset, ctx)
    }

  def run(source: Source[(AssetWithData, PipelineContext), NotUsed]): Future[Seq[AssetWithData]] = {
    source
      .via(extractMetadataFlow)
      .async
      .via(fileStoreFlow)
      .map { case (dataAsset, _) => dataAsset }
      .runWith(Sink.seq)
  }

  def shutdown(): Unit = {
    system.terminate()
  }
}
