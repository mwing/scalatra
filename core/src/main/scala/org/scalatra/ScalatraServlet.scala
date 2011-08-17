package org.scalatra

import javax.servlet._
import javax.servlet.http._
import java.net.URL
import scala.util.DynamicVariable
import scala.util.matching.Regex
import scala.collection.mutable.HashSet
import scala.collection.JavaConversions._
import scala.xml.NodeSeq

abstract class ScalatraServlet
  extends HttpServlet
  with ScalatraKernel
  with Initializable
{
  import ScalatraKernel._

  override def service(request: HttpServletRequest, response: HttpServletResponse) = handle(request, response)

  /**
   * Defines the request path to be matched by routers.  The default
   * definition is optimized for `path mapped` servlets (i.e., servlet
   * mapping ends in `&#47;*`).  The route should match everything matched by
   * the `&#47;*`.  In the event that the request URI equals the servlet path
   * with no trailing slash (e.g., mapping = `/admin&#47;*`, request URI =
   * '/admin'), a '/' is returned.
   *
   * All other servlet mappings likely want to return request.getServletPath.
   * Custom implementations are allowed for unusual cases.
   */
  def requestPath = request.getPathInfo match {
    case pathInfo: String => pathInfo
    case null => "/"
  }

  /**
   * Invoked when no route matches.  By default, calls `serveStaticResource()`,
   * and if that fails, calls `resourceNotFound()`.
   *
   * This action can be overridden by a notFound block.
   */
  protected var doNotFound: Action = () => {
    serveStaticResource() getOrElse resourceNotFound()
  }

  /**
   * Attempts to find a static resource matching the request path.  Override
   * to return None to stop this.
   */
  protected def serveStaticResource(): Option[Any] =
    servletContext.resource(request) map { _ =>
      servletContext.getNamedDispatcher("default").forward(request, response)
    }

  /**
   * Called by default notFound if no routes matched and no static resource
   * could be found.
   */
  protected def resourceNotFound(): Any = {
    response.setStatus(404)
    if (isDevelopmentMode){
      val error = "Requesting \"%s %s\" on servlet \"%s\" but only have: %s"
      response.getWriter println error.format(
        request.getMethod,
        Option(request.getPathInfo) getOrElse "/",
        request.getServletPath,
        routes.entryPoints.mkString("<ul><li>", "</li><li>", "</li></ul>"))
    }
  }

  def servletContext: ServletContext = getServletContext

  type Config = ServletConfig

  private var _kernelName: String = _
  def kernelName = _kernelName

  override def init(config: ServletConfig) = {
    super.init(config)
    _kernelName = "servlet:"+config.getServletName
    initialize(config) // see Initializable.initialize for why
  }

  override def initialize(config: ServletConfig): Unit = super.initialize(config)
}
