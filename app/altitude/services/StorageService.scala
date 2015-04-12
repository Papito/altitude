package altitude.services

import altitude.dao.StorageDao
import altitude.models.Storage

class StorageService extends BaseService[Storage]{
  override val DAO: StorageDao = new StorageDao
}
