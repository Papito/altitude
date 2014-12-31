package dao.manager.mongo

import dao.mongo.BaseDao
import models.Asset
import reactivemongo.bson.BSONObjectID

class LibraryDao extends BaseDao[Asset, BSONObjectID]("assets") with dao.manager.LibraryDao
