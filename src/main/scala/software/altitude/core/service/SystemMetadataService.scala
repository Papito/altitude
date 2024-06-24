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
    txManager.asReadOnly[Int] {
      try {
        read.version
      }
      catch {
        case _: SQLException => {
          /* Uncomment this if you get "current transaction is aborted, commands ignored until end of transaction block".
             It means the select query failed when it should not have, but the exception itself is normal for new installations
             AND tests (which makes it super-annoying)

             println(ex)
           */

          0
        }
        case ex: Exception => throw ex
      }
    }
  }

  def versionUp(): Unit = {
    val toVersion = version + 1

    txManager.withTransaction {
      systemMetadataDao.updateVersion(toVersion = toVersion)
    }
  }

  def read: SystemMetadata = {
    txManager.asReadOnly[SystemMetadata] {
      systemMetadataDao.getById(SystemMetadataDao.SYSTEM_RECORD_ID.toString)
    }

  }
}
