package software.altitude.core.dao

import software.altitude.core.dao.jdbc.BaseDao
import software.altitude.core.models.SystemMetadata


trait SystemMetadataDao extends BaseDao {
  def version: Int
  def read: SystemMetadata
}
