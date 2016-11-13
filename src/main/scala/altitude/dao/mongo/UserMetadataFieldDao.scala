package altitude.dao.mongo

import altitude.Altitude

class UserMetadataFieldDao (val app: Altitude) extends BaseMongoDao("metadata_fields") with altitude.dao.UserMetadataFieldDao
