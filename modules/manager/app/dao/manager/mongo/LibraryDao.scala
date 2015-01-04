package dao.manager.mongo

import dao.common.mongo.BaseMongoDao
import models.Asset

class LibraryDao extends BaseMongoDao[Asset, String]("assets") with dao.manager.LibraryDao
