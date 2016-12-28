package altitude.dao.postgres

import altitude.Altitude

class MetadataFieldDao(app: Altitude) extends altitude.dao.jdbc.MetadataFieldDao(app) with Postgres