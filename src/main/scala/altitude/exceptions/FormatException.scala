package altitude.exceptions

import altitude.models.Asset

case class FormatException(asset: Asset) extends RuntimeException()
