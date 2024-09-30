package org.hyperagents.yggdrasil.coap;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.elements.util.StringUtil;

/**
 * CoapServer class specific for Yggdrasil, has specific routing rules.
 */
public class YggdrasilCoAPServer extends CoapServer {
  private final CoapEntityHandler handler;

  /**
   * Default constructor for Yggdrasil coapServer.
   */
  public YggdrasilCoAPServer(final CoapEntityHandler handler) {
    super();
    this.handler = handler;
    setupYggdrasil();
  }

  private void setupYggdrasil() {
    CoapResource workspaces = new Workspaces(handler);
    this.add(workspaces);
  }

  @Override
  protected Resource createRoot() {
    return new YggdrasilRootResource();
  }


  class YggdrasilRootResource extends CoapResource {
    public YggdrasilRootResource() {
      super("");
      setVersion(StringUtil.CALIFORNIUM_VERSION);
    }

    @Override
    public void handleGET(final CoapExchange exchange) {
      handler.handleGetEntity(exchange);
    }
  }

  static class ArtifactHandlerResoure extends CoapResource {
    CoapEntityHandler handler;

    public ArtifactHandlerResoure(CoapEntityHandler handler, String name) {
      super(name);
      this.handler = handler;
    }

    @Override
    public void handleGET(final CoapExchange exchange) {
      handler.handleGetEntity(exchange);
    }

    @Override
    public Resource getChild(String name) {
      return this;
    }
  }

  static class WorkspaceHandlerResource extends CoapResource {
    CoapEntityHandler handler;
    ArtifactHandlerResoure artifactHandlerResource;


    public WorkspaceHandlerResource(CoapEntityHandler handler,
                                    ArtifactHandlerResoure artifactHandler, String name) {
      super(name, false);
      this.handler = handler;
      this.artifactHandlerResource = artifactHandler;
    }

    @Override
    public void handleGET(final CoapExchange exchange) {
      handler.handleGetEntity(exchange);
    }

    @Override
    public void handlePOST(final CoapExchange exchange) {
      handler.handlePostWorkspace(exchange);
    }

    @Override
    public void handleDELETE(final CoapExchange exchange) {
      handler.handleDeleteEntity(exchange);

    }

    @Override
    public Resource getChild(String name) {
      // need to be able to join and leave
      // need to be able to traverse -> artifacts
      if (name.equals("artifacts")) {
        return artifactHandlerResource;
      }
      return this;
    }
  }

  static class Workspaces extends CoapResource {
    private final WorkspaceHandlerResource workspaceHandlerResource;

    public Workspaces(CoapEntityHandler handler) {
      super("workspaces");
      final var artifactHandlerResoure = new ArtifactHandlerResoure(handler, "*");
      this.workspaceHandlerResource = new WorkspaceHandlerResource(handler, artifactHandlerResoure,
          "*");
    }

    @Override
    public Resource getChild(String name) {
      return workspaceHandlerResource;
    }
  }
}
