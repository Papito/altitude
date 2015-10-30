package integration

import altitude.exceptions.NotFoundException
import altitude.models.Folder
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._
import integration.util.Text
import altitude.{Const => C}

@DoNotDiscover class FolderTests (val config: Map[String, String]) extends IntegrationTestCore {
  test("hierarchy") {
    /*
      folder1
        folder1_1
        folder1_2
      */
    val folder1: Folder = altitude.service.folder.add(
      Folder(name = "folder1"))

    folder1.parentId should be(None)

    val folder1_1: Folder = altitude.service.folder.add(
      Folder(name = "folder1_1", parentId = folder1.id))

    folder1_1.parentId should not be None

    val folder1_2: Folder = altitude.service.folder.add(
      Folder(name = "folder1_2", parentId = folder1.id))

    folder1_2.parentId should not be None
    folder1_2.parentId.get should be(folder1.id.get)

    /*
      folder2
        folder2_1
          folder2_1_1
        folder2_2
      */
    val folder2: Folder = altitude.service.folder.add(
      Folder(name = "folder2"))

    folder2.parentId should be(None)

    val folder2_1: Folder = altitude.service.folder.add(
      Folder(name = "folder2_1", parentId = folder2.id))

    folder2_1.parentId should not be None
    folder2_1.parentId.get should be(folder2.id.get)

    val folder2_1_1: Folder = altitude.service.folder.add(
      Folder(name = "folder2_1_1", parentId = folder2_1.id))

    folder2_1_1.parentId should not be None
    folder2_1_1.parentId.get should be(folder2_1.id.get)


    val folder2_2: Folder = altitude.service.folder.add(
      Folder(name = "folder2_2", parentId = folder2.id))

    folder2_2.parentId should not be None
    folder2_2.parentId.get should be(folder2.id.get)

    val folders = altitude.service.folder.getHierarchy()
    folders.size should be(2)
    folders.head.children.size should be (2)
    folders(1).children.size should be (2)
    folders(1).children.head.children.size should be(1)
    folders(1).children(1).children.size should be(0)
  }

  test("delete folder") {
    /*
  folder1
    folder1_1
      folder1_1_1
        folder1_1_1_1
        folder1_1_1_2
    folder1_2
  */
    val folder1: Folder = altitude.service.folder.add(
      Folder(name = "folder1"))

    val folder1_1: Folder = altitude.service.folder.add(
      Folder(name = "folder1_1", parentId = folder1.id))

    val folder1_1_1: Folder = altitude.service.folder.add(
      Folder(name = "folder1_1_1", parentId = folder1_1.id))

    val folder1_1_1_1: Folder = altitude.service.folder.add(
      Folder(name = "folder1_1_1_1", parentId = folder1_1_1.id))

    val folder1_1_1_2: Folder = altitude.service.folder.add(
      Folder(name = "folder1_1_1_2", parentId = folder1_1_1.id))

    val folder1_2: Folder = altitude.service.folder.add(
      Folder(name = "folder1_2", parentId = folder1.id))

    val deleted = altitude.service.folder.deleteById(folder1.id.get)
    deleted should be(6)

    intercept[NotFoundException] {
      altitude.service.folder.getById(folder1.id.get)
    }

    // children should be removed as well
    intercept[NotFoundException] {
      altitude.service.folder.getById(folder1_1.id.get)
    }
    intercept[NotFoundException] {
      altitude.service.folder.getById(folder1_1_1.id.get)
    }
    intercept[NotFoundException] {
      altitude.service.folder.getById(folder1_1_1_1.id.get)
    }
    intercept[NotFoundException] {
      altitude.service.folder.getById(folder1_1_1_2.id.get)
    }
    intercept[NotFoundException] {
      altitude.service.folder.getById(folder1_2.id.get)
    }
  }
}
