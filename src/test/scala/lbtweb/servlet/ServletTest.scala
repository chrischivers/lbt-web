package lbtweb.servlet

import org.scalatest.FunSuite
import org.scalatra.test.scalatest.ScalatraSuite

class ServletTest extends FunSuite with ScalatraSuite {

  addServlet(classOf[LbtServlet], "/*")

  test("Should produce 404 for unspecified paths") {
    get("/") {
      status should equal(404)
    }
  }

  test("Should produce 200 for routeList") {
    get("/routelist") {
      status should equal(200)
    }
  }
}