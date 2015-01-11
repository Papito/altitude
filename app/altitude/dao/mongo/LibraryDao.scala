package altitude.dao.mongo

import altitude.models.Asset

class LibraryDao extends BaseMongoDao[Asset, String]("assets") with altitude.dao.LibraryDao
