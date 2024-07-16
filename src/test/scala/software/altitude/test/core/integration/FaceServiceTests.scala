package software.altitude.test.core.integration

import org.scalatest.DoNotDiscover
import org.scalatest.matchers.must.Matchers.not
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import software.altitude.core.models._
import software.altitude.core.util.Query
import software.altitude.core.{Altitude, DuplicateException, IllegalOperationException, NotFoundException, RequestContext, Util, Const => C}
import software.altitude.test.core.IntegrationTestCore

@DoNotDiscover class FaceServiceTests(override val testApp: Altitude) extends IntegrationTestCore {

  test("Faces are recognized in an image", Focused) {
  }
}
