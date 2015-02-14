package altitude.dao.mongo

import altitude.models.Asset

class LibraryDao extends BaseMongoDao("assets") with altitude.dao.LibraryDao
