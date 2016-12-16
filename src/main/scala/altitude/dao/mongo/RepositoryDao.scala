package altitude.dao.mongo

import altitude.Altitude

class RepositoryDao(val app: Altitude) extends BaseMongoDao("repositories") with altitude.dao.RepositoryDao
