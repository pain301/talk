interface Servlet {
  // Called by the servlet container to allow the servlet to respond to a request.
  public void service(ServletRequest req, ServletResponse res)
  throws ServletException, IOException;
}

abstract class GenericServlet implements Servlet, ServletConfig,
  java.io.Serializable {
  public abstract void service(ServletRequest req, ServletResponse res)
  throws ServletException, IOException;
}

abstract class HttpServlet extends GenericServlet {
  // Dispatches client requests to the protected service method. There's no need to
  // override this method.
  public void service(ServletRequest req, ServletResponse res)
    throws ServletException, IOException
  {
    HttpServletRequest  request;
    HttpServletResponse response;
        
    if (!(req instanceof HttpServletRequest &&
      res instanceof HttpServletResponse)) {
      throw new ServletException("non-HTTP request or response");
    }

    request = (HttpServletRequest) req;
    response = (HttpServletResponse) res;

    service(request, response);
  }
}
