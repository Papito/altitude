package altitude.dao.mongo

import altitude.Altitude

class TrashDao(val app: Altitude) extends BaseMongoDao("trash") with altitude.dao.TrashDao