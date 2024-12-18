package software.altitude.core

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.scaladsl.Source
import software.altitude.core.dao.jdbc.BaseDao
import software.altitude.core.models.Asset
import software.altitude.core.models.AssetWithData

import scala.concurrent.Future

class PipelineSystem(app: Altitude) {

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "pipeline-system")

  private val extractMetadataFlow: Flow[AssetWithData, AssetWithData, NotUsed] = Flow[AssetWithData].map {
    dataAsset =>
      val assetId = BaseDao.genId
      // val userMetadata = app.service.metadata.cleanAndValidate(dataAsset.asset.userMetadata)
      val extractedMetadata = app.service.metadataExtractor.extract(dataAsset.data)
      val publicMetadata = Asset.getPublicMetadata(extractedMetadata)

      val asset: Asset = dataAsset.asset.copy(
        id = Some(assetId),
        // userMetadata = userMetadata,
        extractedMetadata = extractedMetadata,
        publicMetadata = publicMetadata
      )

      AssetWithData(asset, dataAsset.data)
  }

  def importPipeline(source: Source[AssetWithData, NotUsed]): Future[AssetWithData] = {
    source
      .via(extractMetadataFlow)
      .runWith(Sink.head)
  }

  def shutdown(): Unit = {
    system.terminate()
  }
}
