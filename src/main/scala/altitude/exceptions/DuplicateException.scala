package altitude.exceptions

import altitude.models.Asset

case class DuplicateException(asset: Asset) extends Exception
