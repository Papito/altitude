package integration.util.dao

import altitude.dao.BaseDao
import altitude.{Altitude, Context}

trait UtilitiesDao extends BaseDao {
  val app: Altitude
  def migrateDatabase(): Unit
  protected def rollback(): Unit
  protected def close(): Unit
  def cleanupTest(): Unit
  def createTransaction(ctx: Context): Unit
}
