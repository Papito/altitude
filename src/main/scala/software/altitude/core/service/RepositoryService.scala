package software.altitude.core.service


import net.codingwell.scalaguice.InjectorExtensions._
import org.apache.commons.io.FilenameUtils
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject
import software.altitude.core.{Const => C, NotFoundException, Context, Altitude}
import software.altitude.core.dao.RepositoryDao
import software.altitude.core.models.{User, BaseModel, Stats, Repository}
import software.altitude.core.transactions.{TransactionId, AbstractTransactionManager}
import software.altitude.core.util.Query

class RepositoryService(val app: Altitude) extends BaseService[Repository] {
  private final val log = LoggerFactory.getLogger(getClass)
  protected val DAO = app.injector.instance[RepositoryDao]
  override protected val txManager = app.injector.instance[AbstractTransactionManager]

  override def getById(id: String)(implicit ctx: Context, txId: TransactionId = new TransactionId): JsObject = {
    throw new NotImplementedError
  }

  def getRepositoryById(id: String)(implicit txId: TransactionId = new TransactionId): Repository = {
    txManager.asReadOnly[JsObject] {

      implicit val context = Context.EMPTY

      DAO.getById(id) match {
        case Some(obj) => obj
        case None => throw NotFoundException(s"Cannot find ID '$id'")
      }
    }
  }
  def addRepository(name: String, fileStoreType: C.FileStoreType.Value, user: User, queryForDup: Option[Query] = None)
         (implicit txId: TransactionId = new TransactionId): JsObject = {

    log.info(s"Creating repository [$name]")

    val workPath = System.getProperty("user.dir")
    log.info(s"Repository [$name] work path: [$workPath]")
    val dataDir = app.config.getString("dataDir")
    log.info(s"Repository [$name] data path: [$dataDir]")
    val dataPath = FilenameUtils.concat(workPath, dataDir)
    log.info(s"Repository [$name] work path")
    log.info(s"Data path: [$dataPath]")

    val repoToSave = Repository(
      name = name,
      rootFolderId = BaseModel.genId,
      triageFolderId = BaseModel.genId,
      fileStoreType = fileStoreType,
      fileStoreConfig = Map(C.Repository.Config.PATH -> dataPath))

    val repo: Repository = super.add(repoToSave, queryForDup)(txId = txId, ctx = new Context(repo = null, user = user))
    implicit val ctx = new Context(repo = repo, user = user)

    log.info(s"Creating repository [${ctx.repo.name}] system folders")
    app.service.folder.add(app.service.folder.getRootFolder)
    app.service.folder.add(app.service.folder.getTriageFolder)

    log.info(s"Setting up repository [${ctx.repo.name}] statistics")
    app.service.stats.createStat(Stats.SORTED_ASSETS)
    app.service.stats.createStat(Stats.SORTED_BYTES)
    app.service.stats.createStat(Stats.TRIAGE_ASSETS)
    app.service.stats.createStat(Stats.TRIAGE_BYTES)
    app.service.stats.createStat(Stats.RECYCLED_ASSETS)
    app.service.stats.createStat(Stats.RECYCLED_BYTES)

    repo
  }
}

