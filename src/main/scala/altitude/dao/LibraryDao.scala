package altitude.dao

import altitude.models.Asset

trait LibraryDao extends BaseDao {
  def addImagePreview(asset: Asset, bytes: Array[Byte]): Option[String]
}
