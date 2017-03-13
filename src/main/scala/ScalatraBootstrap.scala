import org.scalatra._
import javax.servlet.ServletContext

import lbtweb.servlet.LbtServlet

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    context.mount(new LbtServlet, "/*")
  }
}
