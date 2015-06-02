package altitude.dao.mongo

import altitude.Altitude

class LibraryDao(val app: Altitude) extends BaseMongoDao("assets") with altitude.dao.LibraryDao
