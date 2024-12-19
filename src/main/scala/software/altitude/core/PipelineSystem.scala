package software.altitude.core

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.stream.scaladsl.FlowWithContext
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.scaladsl.Source
import software.altitude.core.dao.jdbc.BaseDao
import software.altitude.core.models.Asset
import software.altitude.core.models.AssetWithData
import software.altitude.core.models.Repository
import software.altitude.core.models.User

import scala.concurrent.Future

case class PipelineContext(repository: Repository, account: User)

class PipelineSystem(app: Altitude) {

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "pipeline-system")

  private val extractMetadataFlow: FlowWithContext[AssetWithData, PipelineContext, AssetWithData, PipelineContext, NotUsed] =
    FlowWithContext[AssetWithData, PipelineContext].mapAsync(parallelism = 1) {
      dataAsset: AssetWithData =>
        val assetId = BaseDao.genId
        val extractedMetadata = app.service.metadataExtractor.extract(dataAsset.data)
        val publicMetadata = Asset.getPublicMetadata(extractedMetadata)

        val updatedAsset = dataAsset.asset.copy(
          id = Some(assetId),
          extractedMetadata = extractedMetadata,
          publicMetadata = publicMetadata
        )

        Future.successful(dataAsset.copy(asset = updatedAsset))
    }

  def importPipeline(source: Source[(AssetWithData, PipelineContext), NotUsed]): Future[(AssetWithData, PipelineContext)] = {
    source
      .via(extractMetadataFlow)
      .runWith(Sink.head)
  }

  def shutdown(): Unit = {
    system.terminate()
  }
}
