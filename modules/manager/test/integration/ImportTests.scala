package integration

import org.scalatest._
import org.scalatestplus.play._
import play.api.Play
import util.log

import scala.io.Source

class ImportTests extends FunSuite with OneAppPerSuite {

  test("List files") {
    val incomingPath = getClass.getResource("../files/incoming").getPath
    global.ManagerGlobal.importService.getImportAssets(path = incomingPath)
  }
}