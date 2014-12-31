package service.manager

//import constants.{const => C}
import models.Asset
import reactivemongo.bson.BSONObjectID

class LibraryService extends BaseService[Asset, BSONObjectID] {
  override val DAO = new dao.manager.mongo.LibraryDao
}
