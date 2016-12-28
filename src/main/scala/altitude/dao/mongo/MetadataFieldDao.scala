package altitude.dao.mongo

import altitude.{Altitude, Const => C}


class MetadataFieldDao(val app: Altitude) extends BaseMongoDao("metadata_fields")
  with altitude.dao.MetadataFieldDao