package dao.manager.mongo

import dao.mongo.BaseDao
import models.Asset

class LibraryDao extends BaseDao[Asset]("assets") with dao.manager.LibraryDao
