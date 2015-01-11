package altitude.manager.dao.postgres

import altitude.common.dao.postgres.BasePostgresDao
import altitude.common.models.Asset

class LibraryDao extends BasePostgresDao[Asset, String]("asset") with altitude.manager.dao.LibraryDao
