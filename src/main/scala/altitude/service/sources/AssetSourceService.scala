package altitude.service.sources

import altitude.models.ImportAsset

trait AssetSourceService {
  def assetIterator(path: String): Iterator[ImportAsset]
  def count(path: String): Int
}
