package software.altitude.core.pipeline

import software.altitude.core.models.Asset
import software.altitude.core.models.AssetWithData
import software.altitude.core.models.Repository
import software.altitude.core.models.User

object PipelineTypes {
  case class PipelineContext(repository: Repository, account: User)
  case class Invalid(payload: Asset, cause: Option[Throwable])

  type TAssetOrInvalid = Either[Asset, Invalid]
  private type TAssetWithDataOrInvalid = Either[AssetWithData, Invalid]
  type TAssetOrInvalidWithContext = (TAssetWithDataOrInvalid, PipelineContext)
  type TAssetWithContext = (AssetWithData, PipelineContext)
}
