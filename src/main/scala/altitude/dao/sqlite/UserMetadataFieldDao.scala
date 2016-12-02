package altitude.dao.sqlite

import altitude.Altitude

class UserMetadataFieldDao(app: Altitude) extends altitude.dao.jdbc.UserMetadataFieldDao(app) with Sqlite