package software.altitude.core.service

import net.codingwell.scalaguice.InjectorExtensions.ScalaInjector
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.altitude.core.Altitude
import software.altitude.core.dao.SystemMetadataDao
import software.altitude.core.models.SystemMetadata
import software.altitude.core.transactions.TransactionManager

import java.sql.SQLException

class SystemMetadataService(val app: Altitude) {
  protected val log: Logger = LoggerFactory.getLogger(getClass)

  private val systemMetadataDao: SystemMetadataDao = app.injector.instance[SystemMetadataDao]
  protected val txManager: TransactionManager = app.txManager

  def version: Int = {
    try {
      read.version
    }
    catch {
      case _: SQLException => 0 // new installation
      case ex: Exception => throw ex
    }
  }

  def versionUp(): Unit = {
    log.info("VERSION UP")
    val toVersion = version + 1
    systemMetadataDao.updateVersion(toVersion = toVersion)
  }

  def read: SystemMetadata = {
    txManager.asReadOnly[SystemMetadata] {
      systemMetadataDao.getById(SystemMetadataDao.SYSTEM_RECORD_ID.toString).get
    }

  }
}
