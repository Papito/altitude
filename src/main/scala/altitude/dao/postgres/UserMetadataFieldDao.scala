package altitude.dao.postgres

import altitude.Altitude

class UserMetadataFieldDao(app: Altitude) extends altitude.dao.jdbc.UserMetadataFieldDao(app) with Postgres