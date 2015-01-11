package altitude.manager.dao.mongo

import altitude.common.dao.mongo.BaseMongoDao
import altitude.common.models.Asset

class LibraryDao extends BaseMongoDao[Asset, String]("assets") with altitude.manager.dao.LibraryDao
