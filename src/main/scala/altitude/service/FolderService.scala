package altitude.service

import altitude.Validators.ModelDataValidator
import altitude.dao.FolderDao
import altitude.models.Folder
import altitude.transactions.TransactionId
import altitude.util.Query
import altitude.{Const => C, _}
import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory
import play.api.libs.json._

object FolderService {
  class FolderValidator
    extends ModelDataValidator(
      required = Some(List(C.Folder.NAME, C.Folder.PARENT_ID)))
}

class FolderService(val app: Altitude) extends BaseService[Folder] {
  private final val log = LoggerFactory.getLogger(getClass)
  override protected val DAO = app.injector.instance[FolderDao]

  override final val CLEANER = Some(
    Cleaners.Cleaner(
      trim = Some(List(C.Folder.NAME, C.Folder.PARENT_ID))))

  override final val VALIDATOR = Some(new FolderService.FolderValidator)

  /**
   * Add a new folder - THIS SHOULD NOT BE USED DIRECTLY. Use <code>addFolder</code>
   */
  override def add(folder: Folder, queryForDup: Option[Query] = None)
                  (implicit ctx: Context, txId: TransactionId = new TransactionId): JsObject = {
    if (isSystemFolder(Some(folder.parentId))) {
      throw new IllegalOperationException("Cannot add a child to a system folder")
    }

    txManager.withTransaction[JsObject] {
      val dupQuery = Query(Map(
        C.Folder.PARENT_ID -> folder.parentId,
        C.Folder.NAME_LC -> folder.nameLowercase))

      try {
        val addedFolder = addPath(super.add(folder, Some(dupQuery)))
        require(addedFolder.path.nonEmpty)
        app.service.fileStore.addFolder(addedFolder)
        addedFolder
      } catch {
        case _: DuplicateException => {
          val ex = ValidationException()
          ex.errors += (C.Folder.NAME -> C.Msg.Warn.DUPLICATE)
          throw ex
        }
      }
    }
  }

  /**
   * Add a new folder
   */
  def addFolder(name: String, parentId: Option[String] = None)
               (implicit ctx: Context, txId: TransactionId = new TransactionId): JsObject = {
    val _parentId = if (parentId.isDefined) parentId.get else ctx.repo.rootFolderId
    val folder = Folder(
      name = name,
      parentId = _parentId)

    add(folder)
  }

  /**
   * Return ALL folders - system and non-system
   */
  override def getAll(implicit ctx: Context, txId: TransactionId = new TransactionId): List[JsObject] = {
    txManager.asReadOnly[List[JsObject]] {
     val wCounts = addAssetCount(DAO.getAll)
     val wPaths = wCounts
     wPaths
    }
  }

  /**
   * Get root folder object for this repository.
   */
  def getRootFolder(implicit ctx: Context, txId: TransactionId = new TransactionId) = Folder(
    id = Some(ctx.repo.rootFolderId),
    parentId = ctx.repo.rootFolderId,
    name = C.Folder.Names.ROOT,
    path = Some(app.service.fileStore.sortedFolderPath)
  )

  /**
   * Get the Triage folder for this repository
   */
  def getTriageFolder(implicit ctx: Context, txId: TransactionId = new TransactionId) = Folder(
    id = Some(ctx.repo.triageFolderId),
    parentId = ctx.repo.rootFolderId,
    name = C.Folder.Names.TRIAGE,
    path = Some(app.service.fileStore.triageFolderPath)
  )

  /**
   * Get all systems folders - the ones the user cannot alter
   */
  def getSystemFolders(implicit ctx: Context, txId: TransactionId = new TransactionId): List[Folder] =
    List(getTriageFolder)

  def isRootFolder(id: String)(implicit ctx: Context, txId: TransactionId = new TransactionId) =
    id == ctx.repo.rootFolderId

  def isTriageFolder(id: String)(implicit ctx: Context, txId: TransactionId = new TransactionId) =
    id == ctx.repo.triageFolderId

  def isSystemFolder(id: Option[String])(implicit ctx: Context, txId: TransactionId = new TransactionId) =
    getSystemFolders.exists(_.id == id)

  /**
   * Given a list of folders, calculate and append asset counts
   */
  private def addAssetCount(folders: List[JsObject])
                           (implicit ctx: Context): List[JsObject] = {
    folders.map { json =>
      val id = (json \ C.Base.ID).as[String]
      val assetCount = flatChildren(id, folders).toSeq.map(_.numOfAssets).sum

      json ++ Json.obj(C.Folder.NUM_OF_ASSETS -> JsNumber(assetCount))
    }
  }

  /**
   * Given a list of folders, calculate and append the folder paths
   */
  private def addPaths(folders: List[JsObject])
                      (implicit ctx: Context, txId: TransactionId): List[JsObject] = {

    txManager.asReadOnly[List[JsObject]] {
    /*
        folders.filter {
          json => (json \ C.Base.ID).as[String] == _parentId }.map { json =>
          // get parent ID (it already has the path calculated)
          val parentFolder = get
          json ++ Json.obj(C.Folder.PATH -> JsNumber(assetCount))
        }
    */
    List()
    }
  }

  private def addPath(folder: Folder)(implicit ctx: Context, txId: TransactionId): Folder = {
    if (isRootFolder(folder.id.get)) {
      return getRootFolder
    }

    if (isTriageFolder(folder.id.get)) {
      return getTriageFolder
    }

    txManager.asReadOnly[Folder] {
      val _pathComponents = pathComponents(folder.id.get).map(_.name)
      val relPath = app.service.fileStore.assemblePath(_pathComponents)

      Folder(
        id = folder.id,
        name = folder.name,
        parentId = folder.parentId,
        path = Some(relPath),
        numOfAssets = folder.numOfAssets)
    }
  }

  /**
   * Return all NON-system folders.
   */
  def getUserFolders(all: List[JsObject] = List())
                      (implicit ctx: Context, txId: TransactionId = new TransactionId): List[JsObject] = {
    txManager.asReadOnly[List[JsObject]] {
      val _all = if (all.isEmpty) getAll else all

      _all.filter(json => {
        val id = (json \ C.Base.ID).asOpt[String]
        !isSystemFolder(id)
      })
    }
  }

  /**
   * Get the folder hierarchy, with an arbitrary folder ID as the root ID to start with.
   */
  def hierarchy(rootId: Option[String] = None, all: List[JsObject] = List())
               (implicit ctx: Context, txId: TransactionId = new TransactionId): List[Folder] = {
    val _rootId = if (rootId.isDefined) rootId.get else ctx.repo.rootFolderId

    txManager.asReadOnly {
      val userFolders = getUserFolders(all)

      val rootEl = userFolders.find(json => (json \ C.Base.ID).as[String] == _rootId)

      isRootFolder(_rootId) || rootEl.isDefined match {
        case true =>
          children(_rootId, userFolders)
        case false =>
          throw NotFoundException(s"Cannot get hierarchy. Root folder $rootId does not exist")
      }
    }
  }

  /**
   * Folder ancestry as a list, top->bottom = root->this folder.
   */
  def pathComponents(folderId: String)
                    (implicit ctx: Context, txId: TransactionId = new TransactionId): List[Folder] = {
    // short-circuit for root folder
    if (isRootFolder(folderId)) {
      return List[Folder]()
    }

    txManager.asReadOnly[List[Folder]] {
      val userFolders = getUserFolders()

      val folderEl = userFolders.find(json => (json \ C.Base.ID).as[String] == folderId)
      val folder: Folder = folderEl.isDefined match {
        case true =>
          Folder.fromJson(folderEl.get)
        case false =>
          throw NotFoundException(s"Folder with ID '$folderId' not found")
      }

      val parents = findParents(folderId = folderId, all = userFolders)

      List(getRootFolder) ::: (folder :: parents).reverse
    }
  }

  /**
   * Get children for the root given, but only a single level - non-recursive
   */
  def immediateChildren(rootId: String, all: List[JsObject] = List())
                       (implicit ctx: Context, txId: TransactionId = new TransactionId): List[Folder] = {

    txManager.asReadOnly[List[Folder]] {
      val userFolders = getUserFolders(all)

      userFolders.filter(json => {
          val id = (json \ C.Base.ID).as[String]
          val parentId = (json \ C.Folder.PARENT_ID).as[String]
          parentId == rootId && !isSystemFolder(Some(id))
        })
        .map{json => Folder.fromJson(json)}
        .sortBy(_.nameLowercase)
    }
  }

  /**
   * Delete a folder by ID, including its children. Does not allow deleting the root folder,
   * or any system folders.
   */
  override def deleteById(id: String)
                         (implicit ctx: Context, txId: TransactionId = new TransactionId): Int = {
    if (isRootFolder(id)) {
      throw new IllegalOperationException("Cannot delete the root folder")
    }

    if (isSystemFolder(Some(id))) {
      throw new IllegalOperationException("Cannot delete system folder")
    }

    txManager.withTransaction[Int] {

      val folder: Folder = getById(id)

      log.info(s"Deleting folder $folder")

      // get the list of tuples - (depth, id), with most-deep first
      val childrenAndDepths: List[(Int, String)] =
        flatChildrenIdsWithDepths(id, getUserFolders()).sortBy(_._1).reverse
                                                        /* ^^^ sort by depth */

      // now delete the children, most-deep first
      txManager.withTransaction[Int] {
        childrenAndDepths.foreach{t =>
          val folderId = t._2
          DAO.deleteById(folderId)}

        app.service.fileStore.deleteFolder(folder)
        // return number of folders deleted
        childrenAndDepths.length
      }
    }
  }

  override def deleteByQuery(query: Query)
                            (implicit ctx: Context, txId: TransactionId = new TransactionId): Int = {
    throw new NotImplementedError("Cannot delete folders by query")
  }

  /**
   * Returns a nested list of children for a given folder.
   * Not a flat list! Subsequent children are in each folders' "children" element.
    */
  private def children(parentId: String, all: List[JsObject])
                      (implicit ctx: Context, txId: TransactionId): List[Folder] = {
    val userFolders = getUserFolders(all)
    val immediateChildren = this.immediateChildren(parentId, userFolders)

    val folders = for (folder <- immediateChildren) yield  {
      val id: String = (folder \ C.Base.ID).as[String]
      val name = (folder \ C.Folder.NAME).as[String]
      val path = (folder \ C.Folder.PATH).as[String]
      val assetCount = (folder \ C.Folder.NUM_OF_ASSETS).as[Int]

      Folder(
        id = Some(id),
        name = name,
        path = Some(path),
        parentId = parentId,
        children = this.children(id, userFolders),
        numOfAssets = assetCount)
    }

    folders.sortBy(_.nameLowercase)
  }

  /**
   * Returns a list of parents for a given folder ID.
   * The list goes from closest parent to farthest.
   */
  private def findParents(folderId: String, all: List[JsObject])
                         (implicit ctx: Context, txId: TransactionId):  List[Folder] = {
    val userFolders = getUserFolders(all)
    val folderEl = userFolders.find(json => (json \ C.Base.ID).as[String] == folderId)

    val parentId = folderEl.isDefined match {
      case true =>
        (folderEl.get \ C.Folder.PARENT_ID).as[String]
      case false =>
        throw NotFoundException(s"Folder with ID '$folderId' not found")
    }

    val parentElements = userFolders filter (json => (json \ C.Base.ID).as[String] == parentId)

    parentElements.isEmpty match {
      case true => List()
      case false =>
        val folder = Folder.fromJson(parentElements.head)
        List(folder) ++ findParents(folderId = folder.id.get, userFolders)
    }
  }

  override def getById(id: String)
                      (implicit ctx: Context, txId: TransactionId = new TransactionId): JsObject = {

    txManager.asReadOnly[JsObject] {
      val folder: Folder = if (isRootFolder(id)) getRootFolder else super.getById(id)
      val ret = addPath(folder)

      require(ret.path.isDefined)
      require(ret.path.get.nonEmpty)
      ret
    }
  }

  /**
   * Get a folder by ID, but with the number of assets in it as well - including child folders
   */
  def getByIdWithChildAssetCounts(id: String, all: List[JsObject] = List())
                                 (implicit ctx: Context, txId: TransactionId = new TransactionId): JsObject = {
    val userFolders = getUserFolders(all)
    val matching = userFolders.filter(j => (j \ C.Base.ID).asOpt[String].contains(id))

    if (matching.isEmpty) {
      throw NotFoundException(s"Base.ID $id not found")
    }

    matching.head
  }

  /**
   * Return all folders, with their depths, as a flat list, for a given parent folder.
   * Specifically, returns a flat list of tuples, where the first element is the depth,
   * relative to parent folder, and the second element is the folder ID.
   */
  def flatChildrenIdsWithDepths(parentId: String, all: List[JsObject] = List(), depth: Int = 0)
                     (implicit ctx: Context, txId: TransactionId = new TransactionId): List[(Int, String)] = {
    val userFolders = getUserFolders(all)

    val childElements = userFolders.filter(j => (j \ C.Folder.PARENT_ID).asOpt[String].contains(parentId))

    // recursively combine with the result of deeper child levels + this one (depth-first)
    (depth, parentId) :: childElements.foldLeft(List[(Int, String)]()) { (res, json) =>
      val folderId = (json \ C.Base.ID).as[String]
      res ++ flatChildrenIdsWithDepths(folderId, userFolders, depth + 1)}
  }

  /**
   * Returns a unique set of folder IDs for one OR more folder ids.
   * The difference between the other method is that folder depths are not returned.
   * It's a "raw" list of folder ids.
   */
  def flatChildrenIds(parentIds: Set[String], all: List[JsObject] = List())
                     (implicit ctx: Context, txId: TransactionId = new TransactionId): Set[String]  =
    parentIds.foldLeft(Set[String]()) {(s, id) => {
      s ++ app.service.folder.flatChildrenIdsWithDepths(parentId = id, all = all).map(_._2).toSet
    }}

  /**
   * Returns flat children as a set of Folder objects
   */
  def flatChildren(parentId: String, all: List[JsObject], depth: Int = 0)
                  (implicit ctx: Context): Set[Folder]  = {
    val parentElements = all filter (json => (json \ C.Base.ID).as[String] == parentId)

    if (parentElements.isEmpty) {
      return Set()
    }

    val parentElement: Folder = parentElements.head
    val childElements = all.filter(j => (j \ C.Folder.PARENT_ID).asOpt[String].contains(parentId))

    // recursively combine with the result of deeper child levels + this one (depth-first)
    (parentElement :: childElements.foldLeft(List[Folder]()) {(res, json) =>
      val folder: Folder = json
      res ++ flatChildren(folder.id.get, all, depth + 1)}).toSet
  }

  /**
   * Move a folder from one parent to another
   */
  def move(folderBeingMovedId: String, destFolderId: String)
          (implicit ctx: Context, txId: TransactionId = new TransactionId): Unit = {

    if (isRootFolder(folderBeingMovedId)) {
      throw new IllegalOperationException("Cannot move the root folder")
    }

    log.debug(s"Moving folder $folderBeingMovedId to $destFolderId")

    // cannot move into itself
    if (folderBeingMovedId == destFolderId) {
      throw IllegalOperationException(s"Cannot move a folder into itself. ID: $folderBeingMovedId")
    }

    txManager.withTransaction {
      // cannot move into own child
      if (flatChildrenIdsWithDepths(folderBeingMovedId, getUserFolders()).map(_._2).contains(destFolderId)) {
        throw IllegalOperationException(s"Cannot move a folder into own child. $destFolderId ID in $folderBeingMovedId path")
      }

      var destFolder: Option[Folder] = None
      // check that the destination folder exists
      try {
        destFolder = Some(getById(destFolderId))
      } catch {
        case ex: NotFoundException => throw ValidationException(s"Destination folder ID $destFolderId does not exist")
      }

      val folderBeingMoved: Folder = getById(folderBeingMovedId)

      // destination parent cannot have folder by the same name
      val dupQuery = Query(Map(
        C.Folder.PARENT_ID -> destFolderId,
        C.Folder.NAME_LC -> folderBeingMoved.nameLowercase))

      val folderForUpdate = Folder(
        parentId = destFolderId,
        name = folderBeingMoved.name,
        path = Some(app.service.fileStore.getFolderPath(folderBeingMoved.name, destFolderId)))

      try {
        updateById(folderBeingMovedId, folderForUpdate, List(C.Folder.PARENT_ID), Some(dupQuery))
      } catch {
        case ex: DuplicateException => throw ValidationException(
          s"Cannot move to '${destFolder.get.name}' as a folder by this name already exists")
      }

      app.service.fileStore.moveFolder(folderBeingMoved, destFolder.get)
    }
  }

  def rename(folderId: String, newName: String)
            (implicit ctx: Context, txId: TransactionId = new TransactionId): Folder = {
    if (isRootFolder(folderId)) {
      throw new IllegalOperationException("Cannot rename the root folder")
    }

    txManager.withTransaction {
      val folder: Folder = getById(folderId)

      // new folder name cannot match existing one
      val dupQuery = Query(Map(
        C.Folder.PARENT_ID -> folder.parentId,
        C.Folder.NAME_LC -> newName.toLowerCase))

      var folderForUpdate: Option[Folder] = None

      try {
        folderForUpdate = Some(Folder(
          parentId = folder.parentId,
          name = newName,
          path = Some(app.service.fileStore.getFolderPath(newName, folder.parentId))))

        updateById(folderId, folderForUpdate.get, List(C.Folder.NAME, C.Folder.NAME_LC), Some(dupQuery))
      } catch {
        case _: DuplicateException =>
          val ex = ValidationException()
          ex.errors += C.Folder.NAME -> C.Msg.Warn.DUPLICATE
          throw ex
      }

      app.service.fileStore.renameFolder(folder, newName)

      /*
        FIXME: this does not update paths for assets, until dynamic paths are implemented
       */

      folderForUpdate.get
    }
  }

  /**
   * Increase a folder's asset count.
   */
  def incrAssetCount(folderId: String, count: Int = 1)
                    (implicit ctx: Context, txId: TransactionId = new TransactionId) = {
    log.debug(s"Incrementing folder $folderId count by $count")

    txManager.withTransaction {
      DAO.increment(folderId, C.Folder.NUM_OF_ASSETS, count)
    }
  }

  /**
   * Decrease a folder's asset count.
   */
  def decrAssetCount(folderId: String, count: Int = 1)
                    (implicit ctx: Context, txId: TransactionId = new TransactionId) = {
    log.debug(s"Decrementing folder $folderId count by $count")

    txManager.withTransaction {
      DAO.decrement(folderId, C.Folder.NUM_OF_ASSETS, count)
    }
  }

  def getSysFolders(all: List[JsObject] = List())
                   (implicit ctx: Context, txId: TransactionId = new TransactionId): Map[String, Folder] = {
    txManager.asReadOnly[Map[String, Folder]] {
      all.isEmpty match {
        case true =>
          val sysFolderIds: Set[String] = getSystemFolders.map(_.id.get).toSet
          val allSysFolders = DAO.getByIds(sysFolderIds)
          getSystemFolderLookup(allSysFolders)
        case false =>
          val allSysFolders = all.filter(json => {
            val id = (json \ C.Base.ID).asOpt[String]
              isSystemFolder(id)
          })
          getSystemFolderLookup(allSysFolders)
      }
    }
  }

  /**
   * Given a list of system folders, create a lookup-by-folder-id map
   */
  private def getSystemFolderLookup(allSysFolders: List[JsObject]): Map[String, Folder] = {
    allSysFolders.map(j => {
      val id = (j \ C.Base.ID).as[String]
      val folder = Folder.fromJson(j)
      id -> folder
    }).toMap
  }
}
