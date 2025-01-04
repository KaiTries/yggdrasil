package org.hyperagents.yggdrasil.coap;

import io.vertx.core.Vertx;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.ObservableResource;
import org.eclipse.californium.core.server.resources.Resource;
import org.hyperagents.yggdrasil.utils.EnvironmentConfig;
import org.hyperagents.yggdrasil.utils.NetworkInterfaceConfig;
import org.hyperagents.yggdrasil.utils.WebSubConfig;

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
                             final EnvironmentConfig environmentConfig,
                             final WebSubConfig notificationConfig) {
    super(coapConfig.getPort());
    // this.coapConfig = coapConfig;
    // this.httpConfig = httpConfig;
    // this.environmentConfig = environmentConfig;
    this.handler = new CoapEntityHandler(vertx, coapConfig, httpConfig, environmentConfig,
        notificationConfig);
    setupYggdrasil(this);
  }

  private void setupYggdrasil(final YggdrasilCoAPServer s) {
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

  static class ArtifactHandlerResource extends CoapResource {
    final CoapEntityHandler handler;

    public ArtifactHandlerResource(final CoapEntityHandler handler, final String name) {
      super(name);
      this.handler = handler;
    }
    @Override
    public void handleGET(final CoapExchange exchange) {
      handler.handleGetArtifact(exchange);
    }
  }

  static class ArtifactsHandlerResoure extends CoapResource {
    final CoapEntityHandler handler;
    final ArtifactHandlerResource artifactHandlerResource;

    public ArtifactsHandlerResoure(final CoapEntityHandler handler,
                                   final ArtifactHandlerResource artifactHandlerResource,
                                   final String name) {
      super(name);
      this.handler = handler;
      this.artifactHandlerResource = artifactHandlerResource;
    }

    @Override
    public void handleGET(final CoapExchange exchange) {
      handler.handleGetArtifacts(exchange);
    }

    @Override
    public Resource getChild(final String name) {
      return artifactHandlerResource;
    }
  }

  static class WorkspaceHandlerResource extends CoapResource {
    final CoapEntityHandler handler;
    final ArtifactsHandlerResoure artifactsHandlerResource;
    final ArtifactHandlerResource artifactHandlerResource;


    public WorkspaceHandlerResource(final CoapEntityHandler handler,
                                    final ArtifactsHandlerResoure artifactsHandler,
                                    final ArtifactHandlerResource artifactHandler,
                                    final String name) {
      super(name, false);
      this.handler = handler;
      this.artifactsHandlerResource = artifactsHandler;
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
        return artifactsHandlerResource;
      }
      return this;
    }
  }

  class Workspaces extends CoapResource implements ObservableResource {
    private final WorkspaceHandlerResource workspaceHandlerResource;
    private final CoapEntityHandler handler;

    public Workspaces(final CoapEntityHandler handler) {
      super("workspaces", true);
      this.setObservable(true);
      System.out.println("workspaces is observable: " + this.isObservable());
      this.handler = handler;

      final var artifactHandlerResource = new ArtifactHandlerResource(handler, "*");
      final var artifactsHandlerResoure = new ArtifactsHandlerResoure(handler,
          artifactHandlerResource, "*");
      this.workspaceHandlerResource = new WorkspaceHandlerResource(handler, artifactsHandlerResoure,
          artifactHandlerResource,
          "*");
      // this.notifyObserverRelations(null);
    }

    public void notifyObservers() {
      notifyObserverRelations(null);
    }

    @Override
    public void handleGET(final CoapExchange exchange) {
        handler.handleGetWorkspaces(exchange);
    }


    @Override
    public Resource getChild(final String name) {
      return workspaceHandlerResource;
    }
  }


}
