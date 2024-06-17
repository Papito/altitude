package software.altitude.core.service.migration

import software.altitude.core.AltitudeAppContext

trait SqliteMigration { this: CoreMigrationService =>
  protected val app: AltitudeAppContext

  override def existingVersion: Int = {
    // cannot open a readonly connection for a non-existing DB
    txManager.withTransaction[Int] {
      DAO.currentVersion
    }
  }

}
