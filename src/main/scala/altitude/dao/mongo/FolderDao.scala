package altitude.dao.mongo

import altitude.{Altitude, Const => C}


class FolderDao(val app: Altitude) extends BaseMongoDao("folders") with altitude.dao.FolderDao