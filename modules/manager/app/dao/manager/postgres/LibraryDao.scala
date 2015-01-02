package dao.manager.postgres

import dao.postgres.BasePostgresDao
import models.Asset

class LibraryDao extends BasePostgresDao[Asset, String]("asset") with dao.manager.LibraryDao
