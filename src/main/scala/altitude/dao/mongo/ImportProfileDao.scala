package altitude.dao.mongo

import altitude.Altitude

class ImportProfileDao(val app: Altitude) extends BaseMongoDao("import_profiles") with altitude.dao.ImportProfileDao
