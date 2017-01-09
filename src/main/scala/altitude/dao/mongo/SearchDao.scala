package altitude.dao.mongo

import altitude.Altitude

class SearchDao(val app: Altitude) extends BaseMongoDao("search_index") with altitude.dao.SearchDao
