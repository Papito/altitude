package dao.manager

import models.Asset

trait LibraryDao {
  def addAsset(asset: Asset): Asset
}
