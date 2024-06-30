package software.altitude.core.service

import akka.actor.ActorSystem
import akka.stream.alpakka.file.DirectoryChange
import akka.stream.alpakka.file.javadsl.DirectoryChangesSource
import org.apache.commons.io.{FileUtils, FilenameUtils}
import org.slf4j.Logger
import software.altitude.core.{Altitude, RequestContext}
import software.altitude.core.models.{ImportAsset, Metadata, Repository, User}
import software.altitude.core.util.Query

import java.io.File
import java.nio.file.{FileSystems, Files}
import java.time.Duration

class ImportWatcherService(val app: Altitude) {

  private val akkaSystem: ActorSystem = akka.actor.ActorSystem()
  val logger: Logger = org.slf4j.LoggerFactory.getLogger(getClass)

  def start(): Unit = {
    val workPath: String = System.getProperty("user.dir")
    val dataDir: String = app.config.getString("dataDir")
    val dataPath: String = FilenameUtils.concat(workPath, dataDir)
    val importPath: String = FilenameUtils.concat(dataPath, "import")
    val fs = FileSystems.getDefault
    val pollingInterval = Duration.ofSeconds(1)
    val maxBufferSize = 100000

    val importDir = new File(new File(app.config.getString("dataDir")), "import")

    if (!importDir.exists()) {
      FileUtils.forceMkdir(importDir)
    }

    val changes = DirectoryChangesSource.create(fs.getPath(importPath), pollingInterval, maxBufferSize)

    changes.runForeach(pair => {
      val changedPath = pair.first
      val change = pair.second
      if (change == DirectoryChange.Creation) {
        System.out.println("Path: " + changedPath + ", Change: " + change)

        val repoResults = app.service.repository.query(new Query())
        if (repoResults.records.nonEmpty) {
          RequestContext.repository.value = Some(repoResults.records.head: Repository)
        }

        val userResults = app.service.user.query(new Query())
        if (userResults.records.nonEmpty) {
          RequestContext.account.value = Some(userResults.records.head: User)
        }

        val file = new File(changedPath.toString)
        val importAsset = new ImportAsset(
          fileName = file.getName,
          data = Files.readAllBytes(changedPath),
          metadata = Metadata()
        )

        val _ =
          try {
            app.service.assetImport.importAsset(importAsset)
          } catch {
            case e: Exception => e.printStackTrace()
          }
      }
    }, akkaSystem)
  }
}
