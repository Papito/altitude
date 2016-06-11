package altitude.dao.mongo

import altitude.Altitude

class StatsDao(val app: Altitude) extends BaseMongoDao("stats") with altitude.dao.StatsDao
