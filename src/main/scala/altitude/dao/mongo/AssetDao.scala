package altitude.dao.mongo

import altitude.Altitude


class AssetDao(val app: Altitude) extends BaseMongoDao("assets") with altitude.dao.AssetDao