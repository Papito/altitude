package integration

import altitude.exceptions.{DuplicateException, NotFoundException}
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

    folder1.parentId should be("0")

    val folder1_1: Folder = altitude.service.folder.add(
      Folder(name = "folder1_1", parentId = folder1.id.get))

    folder1_1.parentId should not be None

    val folder1_2: Folder = altitude.service.folder.add(
      Folder(name = "folder1_2", parentId = folder1.id.get))

    folder1.id should contain(folder1_2.parentId)

    /*
      folder2
        folder2_1
          folder2_1_1
        folder2_2
      */
    val folder2: Folder = altitude.service.folder.add(
      Folder(name = "folder2"))

    folder2.parentId should be("0")

    val folder2_1: Folder = altitude.service.folder.add(
      Folder(name = "folder2_1", parentId = folder2.id.get))

    folder2.id should contain(folder2_1.parentId)

    val folder2_1_1: Folder = altitude.service.folder.add(
      Folder(name = "folder2_1_1", parentId = folder2_1.id.get))

    folder2_1.id should contain(folder2_1_1.parentId)

    val folder2_2: Folder = altitude.service.folder.add(
      Folder(name = "folder2_2", parentId = folder2.id.get))

    folder2.id should contain(folder2_2.parentId)

    val folders = altitude.service.folder.getHierarchy()
    folders.size should be(2)
    folders.head.children.size should be (2)
    folders(1).children.size should be (2)
    folders(1).children.head.children.size should be(1)
    folders(1).children(1).children.size should be(0)

    // check immediate children of the second folder
    val immediateChildren = altitude.app.service.folder.getImmediateChildren(rootId = folder2_1.id.get)
    immediateChildren.size should be (1)
  }

  test("duplicate") {
    val folder: Folder = altitude.service.folder.add(Folder(name = "folder1"))

    intercept[DuplicateException] {
      altitude.service.folder.add(Folder(name = folder.name))
    }

    val folders = altitude.service.folder.getHierarchy()
    folders.size should be(1)
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
      Folder(name = "folder1_1", parentId = folder1.id.get))

    val folder1_1_1: Folder = altitude.service.folder.add(
      Folder(name = "folder1_1_1", parentId = folder1_1.id.get))

    val folder1_1_1_1: Folder = altitude.service.folder.add(
      Folder(name = "folder1_1_1_1", parentId = folder1_1_1.id.get))

    val folder1_1_1_2: Folder = altitude.service.folder.add(
      Folder(name = "folder1_1_1_2", parentId = folder1_1_1.id.get))

    val folder1_2: Folder = altitude.service.folder.add(
      Folder(name = "folder1_2", parentId = folder1.id.get))

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
