package altitude.dao.mongo

import altitude.Altitude

class StatDao(val app: Altitude) extends BaseMongoDao("stats") with altitude.dao.StatDao
