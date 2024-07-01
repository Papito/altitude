package software.altitude.test.core.integration

import org.scalatest.DoNotDiscover
import org.scalatest.matchers.must.Matchers.contain
import org.scalatest.matchers.must.Matchers.empty
import org.scalatest.matchers.must.Matchers.equal
import org.scalatest.matchers.must.Matchers.not
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import software.altitude.core.IllegalOperationException
import software.altitude.core.NotFoundException
import software.altitude.core.RequestContext
import software.altitude.core.ValidationException
import software.altitude.core.models.Folder
import software.altitude.test.core.IntegrationTestCore

@DoNotDiscover class FolderServiceTests (val config: Map[String, Any]) extends IntegrationTestCore {

  test("Folder hierarchy should contain all top level and child folders") {
    /*
      folder1
        folder1_1
        folder1_2
      */

    val folder1: Folder = altitudeApp.service.library.addFolder("folder1")

    folder1.parentId shouldEqual RequestContext.repository.value.get.rootFolderId

    val folder1_1: Folder = altitudeApp.service.library.addFolder(
      name = "folder1_1", parentId = folder1.id)

        folder1_1.parentId should not be None

        val folder1_2: Folder = altitudeApp.service.library.addFolder(
          name = "folder1_2", parentId = folder1.id)

        folder1.id should contain(folder1_2.parentId)

        /*
          folder2
            folder2_1
              folder2_1_1
            folder2_2
          */
        val folder2: Folder = altitudeApp.service.library.addFolder(
          name = "folder2")

        folder2.parentId shouldEqual RequestContext.repository.value.get.rootFolderId

        val folder2_1: Folder = altitudeApp.service.library.addFolder(
          name = "folder2_1", parentId = folder2.id)

        folder2.id should contain(folder2_1.parentId)

        val folder2_1_1: Folder = altitudeApp.service.library.addFolder(
          name = "folder2_1_1", parentId = folder2_1.id)

        folder2_1.id should contain(folder2_1_1.parentId)

        val folder2_2: Folder = altitudeApp.service.library.addFolder(
          name = "folder2_2", parentId = folder2.id)

        folder2.id should contain(folder2_2.parentId)

        val hierarchy = altitudeApp.service.folder.hierarchy()
        hierarchy.length shouldBe 2
        hierarchy.head.children.length shouldBe 2
        hierarchy(1).children.length shouldBe 2
        hierarchy(1).children.head.children.length shouldBe 1
        hierarchy(1).children(1).children.length shouldBe 0

        // check immediate children of the second folder
        val immediateChildren = altitudeApp.app.service.folder.immediateChildren(rootId = folder2_1.persistedId)
        immediateChildren.length shouldBe 1

        // check breadcrumb
        val path = altitudeApp.app.service.folder.pathComponents(folderId = folder2_1_1.persistedId)
        path.length shouldBe 4
        path(1).id shouldBe folder2.id
        path.last.id shouldBe folder2_1_1.id

        // SECOND REPO
        val repo2 = testContext.persistRepository()
        switchContextRepo(repo2)

        val hierarchy2 = altitudeApp.service.folder.hierarchy()
        hierarchy2 shouldBe empty
  }

  test("Hierarchy for a bad root ID should fail") {
    intercept[NotFoundException] {
      altitudeApp.service.folder.hierarchy(rootId = Some("bogus"))
    }
  }

  test("Root folder path should be empty") {
    val path: List[Folder] = altitudeApp.app.service.folder.pathComponents(folderId = RequestContext.repository.value.get.rootFolderId)
    path.length should equal(0)
  }

  test("Getting the path for a bad folder should fail") {
    intercept[NotFoundException] {
      altitudeApp.app.service.folder.pathComponents(folderId = "bogus")
    }
  }

  test("Adding a duplicate folder under the same parent should fail") {
    val folder: Folder = altitudeApp.service.library.addFolder("folder1")

    intercept[ValidationException] {
      altitudeApp.service.library.addFolder(folder.name)
    }

    val folders = altitudeApp.service.folder.hierarchy()
    folders.length shouldBe 1
  }

  test("Invalid folder names should fail") {
    intercept[ValidationException] {
      altitudeApp.service.library.addFolder("")
    }
    intercept[ValidationException] {
      altitudeApp.service.library.addFolder(" ")
    }
    intercept[ValidationException] {
      altitudeApp.service.library.addFolder(" ")
    }
    intercept[ValidationException] {
      altitudeApp.service.library.addFolder("\t \t   ")
    }
  }

  test("New folders  should be free of user-entered space characters") {
    val folder1: Folder = altitudeApp.service.library.addFolder(" folder  ")
    folder1.name shouldEqual "folder"

    val folder2:Folder = altitudeApp.service.library.addFolder(" Folder one \n")
    folder2.name shouldEqual "Folder one"
  }

  test("Deleting a folder should also remove all children") {
    /*
  folder1
    folder1_1
      folder1_1_1
        folder1_1_1_1
        folder1_1_1_2
    folder1_2
  */
    val folder1: Folder = altitudeApp.service.library.addFolder("folder1")

    val folder1_1: Folder = altitudeApp.service.library.addFolder(
      name = "folder1_1", parentId = folder1.id)

    val folder1_1_1: Folder = altitudeApp.service.library.addFolder(
      name = "folder1_1_1", parentId = folder1_1.id)

    val folder1_1_1_1: Folder = altitudeApp.service.library.addFolder(
      name = "folder1_1_1_1", parentId = folder1_1_1.id)

    val folder1_1_1_2: Folder = altitudeApp.service.library.addFolder(
      name = "folder1_1_1_2", parentId = folder1_1_1.id)

    val folder1_2: Folder = altitudeApp.service.library.addFolder(
      name = "folder1_2", parentId = folder1.id)

    altitudeApp.service.library.deleteFolderById(folder1.persistedId)

    intercept[NotFoundException] {
      altitudeApp.service.folder.getById(folder1.persistedId)
    }

    // children should be removed as well
    intercept[NotFoundException] {
      altitudeApp.service.folder.getById(folder1_1.persistedId)
    }
    intercept[NotFoundException] {
      altitudeApp.service.folder.getById(folder1_1_1.persistedId)
    }
    intercept[NotFoundException] {
      altitudeApp.service.folder.getById(folder1_1_1_1.persistedId)
    }
    intercept[NotFoundException] {
      altitudeApp.service.folder.getById(folder1_1_1_2.persistedId)
    }
    intercept[NotFoundException] {
      altitudeApp.service.folder.getById(folder1_2.persistedId)
    }
    intercept[NotFoundException] {
      altitudeApp.service.folder.getById(folder1.persistedId)
    }
  }

  test("Deleting a non-exisiting folder should fail with a NOT FOUND") {
    intercept[NotFoundException] {
      altitudeApp.service.library.deleteFolderById("bogus")
    }
  }

  test("Deleting the root folder should fail") {
    intercept[IllegalOperationException] {
      altitudeApp.service.library.deleteFolderById(RequestContext.repository.value.get.rootFolderId)
    }
  }

  test("Moving a folder") {
    /*
  folder1
    folder1_1
      folder1_1_1
    folder1_2
  folder2
  */
    val folder1: Folder = altitudeApp.service.library.addFolder("folder1")

    val folder1_1: Folder = altitudeApp.service.library.addFolder(
      name = "folder1_1", parentId = folder1.id)

    val folder1_1_1: Folder = altitudeApp.service.library.addFolder(
      name = "folder1_1_1", parentId = folder1_1.id)

    altitudeApp.service.library.addFolder(
      name = "folder1_2", parentId = folder1.id)

    val folder2: Folder = altitudeApp.service.library.addFolder("folder2")

    // assert initial state
    // target
    altitudeApp.app.service.folder.immediateChildren(rootId = folder2.persistedId).length shouldBe 0
    // source
    altitudeApp.app.service.folder.immediateChildren(rootId = folder1_1.persistedId).length shouldBe 1

    // move folder1_1_1 to folder2
    altitudeApp.service.library.moveFolder(folder1_1_1.persistedId, folder2.persistedId)
    // target
    altitudeApp.app.service.folder.immediateChildren(rootId = folder2.persistedId).length shouldBe 1
    // source
    altitudeApp.app.service.folder.immediateChildren(rootId = folder1_1.persistedId).length shouldBe 0
  }

  test("Moving a folder to repository root should work") {
    /*
  folder1
    folder1_1
      folder1_1_1
  folder2
  */
    val folder1: Folder = altitudeApp.service.library.addFolder("folder1")

    val folder1_1: Folder = altitudeApp.service.library.addFolder(
      name = "folder1_1", parentId = folder1.id)

    val folder1_1_1: Folder = altitudeApp.service.library.addFolder(
      name = "folder1_1_1", parentId = folder1_1.id)

    altitudeApp.service.library.addFolder("folder2")

    // assert initial state
    altitudeApp.app.service.folder.immediateChildren(rootId = RequestContext.repository.value.get.rootFolderId).length shouldBe 2

    altitudeApp.service.library.moveFolder(folder1_1_1.persistedId, RequestContext.repository.value.get.rootFolderId)
    altitudeApp.app.service.folder.immediateChildren(rootId = RequestContext.repository.value.get.rootFolderId).length shouldBe 3

    altitudeApp.service.library.moveFolder(folder1_1.persistedId, RequestContext.repository.value.get.rootFolderId)
    altitudeApp.app.service.folder.immediateChildren(rootId = RequestContext.repository.value.get.rootFolderId).length shouldBe 4
  }

  test("Illegal folder move actions should throw") {
    /*
    folder1
      folder1_1
        folder1_1_1
    folder2
        folder1_1_1
    folder3
        FOLDER1_1_1
    */
    val folder1: Folder = altitudeApp.service.library.addFolder("folder1")

    val folder1_1: Folder = altitudeApp.service.library.addFolder(
      name = "folder1_1", parentId = folder1.id)

    val folder1_1_1: Folder = altitudeApp.service.library.addFolder(
      name = "folder1_1_1", parentId = folder1_1.id)

    val folder2: Folder = altitudeApp.service.library.addFolder("folder2")

    // create folder1_1_1 as a duplicate under a different parent
    altitudeApp.service.library.addFolder(
      name = "folder1_1_1", parentId = folder2.id)

    val folder3: Folder = altitudeApp.service.library.addFolder("folder3")

    // create folder1_1_1 as a duplicate under a different parent
    altitudeApp.service.library.addFolder(
      name = "FOLDER1_1_1", parentId = folder3.id)

    // move into itself
    intercept[IllegalOperationException] {
      altitudeApp.service.library.moveFolder(folder1.persistedId, folder1.persistedId)
    }

    // move into a child
    intercept[IllegalOperationException] {
      altitudeApp.service.library.moveFolder(folder1.persistedId, folder1_1_1.persistedId)
    }

    // move into a parent with the same immediate child name
    intercept[ValidationException] {
      altitudeApp.service.library.moveFolder(folder1_1_1.persistedId, folder2.persistedId)
    }

    // move into a parent with the same immediate child name (different casing_
    intercept[ValidationException] {
      altitudeApp.service.library.moveFolder(folder1_1_1.persistedId, folder3.persistedId)
    }

    // move into a folder that does not exist
    intercept[ValidationException] {
    altitudeApp.service.library.moveFolder(folder1.persistedId, "bogus")
    }
  }

  test("Rename a folder") {
    val folder1: Folder = altitudeApp.service.library.addFolder("folder")

    altitudeApp.service.library.renameFolder(folder1.persistedId, "newName")

    val renamedFolder: Folder = altitudeApp.service.folder.getById(folder1.persistedId)
    renamedFolder.name shouldEqual "newName"
  }

  test("Folder name casing can be changed") {
    val folder1: Folder = altitudeApp.service.library.addFolder("folder")

    altitudeApp.service.library.renameFolder(folder1.persistedId, "Folder")

    val renamedFolder: Folder = altitudeApp.service.folder.getById(folder1.persistedId)
    renamedFolder.name shouldEqual "Folder"
  }

  test("Illegal rename actions should throw") {
    val folder1: Folder = altitudeApp.service.library.addFolder("folder")

    intercept[ValidationException] {
      altitudeApp.service.library.renameFolder(folder1.persistedId, folder1.name)
    }

    // rename a system folder
    intercept[IllegalOperationException] {
      altitudeApp.service.library.renameFolder(RequestContext.repository.value.get.rootFolderId, folder1.name)
    }
  }
}
