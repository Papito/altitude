package altitude.dao.sqlite

import altitude.models.Asset
import altitude.{Const => C, Altitude}
import org.joda.time.DateTime

class AssetDao(app: Altitude) extends altitude.dao.jdbc.AssetDao(app) with Sqlite {

  override protected def setRecycledAtProperty(asset: Asset, rec: Map[String, AnyRef]): Asset = {
    val recycledAtSeconds = rec.getOrElse(C.Base.UPDATED_AT, 0).asInstanceOf[Int]
    if (recycledAtSeconds != 0) {
      asset.recycledAt = new DateTime(recycledAtSeconds.toLong * 1000)
    }

    asset
  }
}

class FolderDao(app: Altitude) extends altitude.dao.jdbc.FolderDao(app) with Sqlite

class MetadataFieldDao(app: Altitude) extends altitude.dao.jdbc.MetadataFieldDao(app) with Sqlite

class MigrationDao(app: Altitude) extends altitude.dao.jdbc.MigrationDao(app) with Sqlite

class RepositoryDao(app: Altitude) extends altitude.dao.jdbc.RepositoryDao(app) with Sqlite



class SearchDao(app: Altitude) extends altitude.dao.jdbc.SearchDao(app) with Sqlite

class StatDao(app: Altitude) extends altitude.dao.jdbc.StatDao(app) with Sqlite {
  override def DEFAULT_SQL_COLS_FOR_SELECT = "*"
}
