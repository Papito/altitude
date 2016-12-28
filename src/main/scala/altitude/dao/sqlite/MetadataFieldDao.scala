package altitude.dao.sqlite

import altitude.Altitude

class MetadataFieldDao(app: Altitude) extends altitude.dao.jdbc.MetadataFieldDao(app) with Sqlite