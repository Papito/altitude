package altitude.dao.postgres

import altitude.models.Asset

class LibraryDao extends BasePostgresDao[Asset, String]("asset") with altitude.dao.LibraryDao
