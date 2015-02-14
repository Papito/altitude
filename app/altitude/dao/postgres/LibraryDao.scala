package altitude.dao.postgres

import altitude.models.Asset

import scala.concurrent.Future

class LibraryDao extends BasePostgresDao("asset") with altitude.dao.LibraryDao
