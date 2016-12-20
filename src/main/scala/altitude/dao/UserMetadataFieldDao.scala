package altitude.dao

import altitude.Context

trait UserMetadataFieldDao extends BaseDao {
  def addConstraintValue(fieldId: String, constraintValue: String)(implicit ctx: Context)
  def deleteConstraintValue(fieldId: String, constraintValue: String)(implicit ctx: Context)
}