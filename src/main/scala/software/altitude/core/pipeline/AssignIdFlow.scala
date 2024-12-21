package software.altitude.core.pipeline

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Flow
import software.altitude.core.Altitude
import software.altitude.core.dao.jdbc.BaseDao
import software.altitude.core.models.Asset
import software.altitude.core.pipeline.PipelineTypes.TAssetOrInvalidWithContext
import software.altitude.core.pipeline.PipelineUtils.debugInfo

object AssignIdFlow {
  def apply(app: Altitude): Flow[TAssetOrInvalidWithContext, TAssetOrInvalidWithContext, NotUsed] =
    Flow[TAssetOrInvalidWithContext].map {
      case (Left(dataAsset), ctx) =>
        val asset: Asset = dataAsset.asset.copy(
          id = Some(BaseDao.genId)
        )
        debugInfo(s"Assigning ID to asset: ${asset.id.get}")

        (Left(dataAsset.copy(asset = asset)), ctx)
      case (Right(invalid), ctx) => (Right(invalid), ctx)
    }
}
