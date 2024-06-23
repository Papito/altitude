package software.altitude.test.core.integration

import software.altitude.core.Altitude
import software.altitude.core.Util
import software.altitude.core.models.AccountType
import software.altitude.core.models.Asset
import software.altitude.core.models.AssetType
import software.altitude.core.models.Folder
import software.altitude.core.models.Metadata
import software.altitude.core.models.Repository
import software.altitude.core.models.User
import software.altitude.core.{Const => C}

class TestContext(val altitude: Altitude) {
  var users: List[User] = List()
  var repositories: List[Repository] = List()
  var assets: List[Asset] = List()

  def makeUser(): User = User(
    email = Util.randomStr(),
    accountType = AccountType.User,
    passwordHash = Some(Util.randomStr(32))
  )

  def persistUser(user: Option[User] = None): User = {
    val userModel = user.getOrElse(makeUser())

    val persistedUser: User = altitude.service.user.add(userModel)
    users = users ::: persistedUser :: Nil

    // if this is the only (or the first user), set current request context
    if (users.length == 1) {
      altitude.service.user.switchContextToUser(persistedUser)
    }

    persistedUser
  }

  def persistRepository(user: Option[User] = None): Repository = {
    if (user.isEmpty && users.length > 1) {
      throw new RuntimeException(
        "Cannot use existing user when there are multiple test context users - must supply a user explicitly")
    }

    if (user.isEmpty && users.isEmpty) {
      persistUser(None)
    }

    // use supplied user, then existing single user, then new user
    val persistedUser = user.getOrElse(users.headOption.getOrElse(makeUser()))

    val persistedRepo: Repository = altitude.service.repository.addRepository(
      name = "Test Repository 1",
      fileStoreType = C.FileStoreType.FS,
      owner = persistedUser)
    repositories = repositories ::: persistedRepo :: Nil

    // if this is the only (or the first repo), set current request context
    if (repositories.length == 1) {
      altitude.service.repository.switchContextToRepository(persistedRepo)
      altitude.service.user.switchContextToUser(persistedUser)
    }

    persistedRepo
  }

  def makeAsset(repository: Option[Repository] = None,
                filename: String = Util.randomStr(50),
                user: Option[User] = None,
                folder: Option[Folder] = None,
                metadata: Metadata = Metadata()): Asset ={
    if (repository.isEmpty && repositories.isEmpty) {
      throw new RuntimeException("Cannot make an asset without a repository previously created")
    }

    if (repository.isEmpty && repositories.length > 1) {
      throw new RuntimeException(
        "Cannot make an asset when there are multiple test context repositories. Specify one explicitly")
    }

    val currentRepo = repository.getOrElse(repositories.headOption.get)
    val folderId = if (folder.isDefined) folder.get.id.get else currentRepo.triageFolderId

    val currentUser = user.getOrElse(this.user)

    Asset(
      userId = currentUser.id.get,
      folderId = folderId,
      assetType = new AssetType(
        mediaType = "mediaType", mediaSubtype = "mediaSubtype", mime = "mime"),
      fileName = filename,
      path = Some(Util.randomStr(50)),
      checksum = Util.randomStr(32),
      metadata = metadata,
      sizeBytes = 1000L)
  }

  def persistAsset(asset: Option[Asset] = None,
                   repository: Option[Repository] = None,
                   user: Option[User] = None,
                   folder: Option[Folder] = None,
                   metadata: Metadata = Metadata()): Asset = {

    if (repository.isEmpty && repositories.length > 1) {
      throw new RuntimeException(
        "Cannot use existing repository when there are  multiple test context repositories - must supply a repository explicitly")
    }

    val persistedRepository = repository.getOrElse(
      repositories.headOption.getOrElse(
        persistRepository(user=user)))

    val assetModel = asset.getOrElse(
      makeAsset(
        repository=Some(persistedRepository), folder=folder, metadata=metadata, user=user))

    val persistedAsset: Asset = altitude.service.library.add(assetModel)

    assets = assets ::: persistedAsset :: Nil

    persistedAsset
  }

  def user: User = {
    if (users.length > 1) {
      throw new RuntimeException("Cannot get a single user when there are multiple test context users")
    }
    users.head
  }

  def repository: Repository = {
    if (repositories.length > 1) {
      throw new RuntimeException("Cannot get a single repository when there are multiple test context repositories")
    }
    repositories.head
  }

}
