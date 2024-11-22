package org.hyperagents.yggdrasil.coap;

import io.vertx.core.Vertx;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.hyperagents.yggdrasil.utils.EnvironmentConfig;
import org.hyperagents.yggdrasil.utils.NetworkInterfaceConfig;

/**
 * CoapServer class specific for Yggdrasil, has specific routing rules.
 */
public class YggdrasilCoAPServer extends CoapServer {
  private final CoapEntityHandler handler;
  // private final NetworkInterfaceConfig coapConfig;
  // private final NetworkInterfaceConfig httpConfig;
  // private final EnvironmentConfig environmentConfig;

  /**
   * Default constructor for Yggdrasil coapServer.
   */
  public YggdrasilCoAPServer(final Vertx vertx,
                             final NetworkInterfaceConfig coapConfig,
                             final NetworkInterfaceConfig httpConfig,
                             final EnvironmentConfig environmentConfig) {
    super(coapConfig.getPort());
    // this.coapConfig = coapConfig;
    // this.httpConfig = httpConfig;
    // this.environmentConfig = environmentConfig;
    this.handler = new CoapEntityHandler(vertx, coapConfig, httpConfig, environmentConfig);
    setupYggdrasil(this);
  }

  private static void setupYggdrasil(final YggdrasilCoAPServer s) {
    final CoapResource workspaces = new Workspaces(s.handler);
    s.add(workspaces);
  }

  @Override
  protected Resource createRoot() {
    return new YggdrasilRootResource();
  }


  class YggdrasilRootResource extends CoapResource {
    public YggdrasilRootResource() {
      super("");
    }

    @Override
    public void handleGET(final CoapExchange exchange) {
      handler.handleGetEntity(exchange);
    }
  }

  static class ArtifactHandlerResoure extends CoapResource {
    final CoapEntityHandler handler;

    public ArtifactHandlerResoure(final CoapEntityHandler handler, final String name) {
      super(name);
      this.handler = handler;
    }

    @Override
    public void handleGET(final CoapExchange exchange) {
      handler.handleGetEntity(exchange);
    }

    @Override
    public Resource getChild(final String name) {
      return this;
    }
  }

  static class WorkspaceHandlerResource extends CoapResource {
    final CoapEntityHandler handler;
    final ArtifactHandlerResoure artifactHandlerResource;


    public WorkspaceHandlerResource(final CoapEntityHandler handler,
                                    final ArtifactHandlerResoure artifactHandler,
                                    final String name) {
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

    }

    @Override
    public Resource getChild(final String name) {
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

    public Workspaces(final CoapEntityHandler handler) {
      super("workspaces");
      final var artifactHandlerResoure = new ArtifactHandlerResoure(handler, "*");
      this.workspaceHandlerResource = new WorkspaceHandlerResource(handler, artifactHandlerResoure,
          "*");
    }

    @Override
    public Resource getChild(final String name) {
      return workspaceHandlerResource;
    }
  }
}
