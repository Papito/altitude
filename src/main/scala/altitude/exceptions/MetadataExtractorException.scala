package altitude.exceptions

import altitude.models.Asset

case class MetadataExtractorException(asset: Asset, ex: Throwable) extends Exception(ex)
