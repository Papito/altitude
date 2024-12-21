package software.altitude.core.pipeline

import software.altitude.core.models.AssetWithData
import software.altitude.core.models.Repository
import software.altitude.core.models.User

object PipelineTypes {
  case class PipelineContext(repository: Repository, account: User)
  case class Invalid(payload: AssetWithData, cause: Option[Throwable])

  private type TAssetOrInvalid = Either[AssetWithData, Invalid]
  type TAssetOrInvalidWithContext = (TAssetOrInvalid, PipelineContext)
  type TAssetWithContext = (AssetWithData, PipelineContext)

}
