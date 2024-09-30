package org.hyperagents.yggdrasil.coap;

import static org.eclipse.californium.core.coap.CoAP.ResponseCode.CONTENT;
import static org.eclipse.californium.core.coap.MediaTypeRegistry.APPLICATION_JSON;
import static org.eclipse.californium.core.coap.MediaTypeRegistry.TEXT_PLAIN;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.elements.config.UdpConfig;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;

/**
 * CoAP server verticle, manages routing.
 */
public class CoapServerVerticle extends AbstractVerticle {

  private CoapServer server;
  private HttpInterfaceConfig httpConfig;

  @Override
  public void start(final Promise<Void> startPromise) {
    this.httpConfig = this.vertx.sharedData()
        .<String, HttpInterfaceConfig>getLocalMap("http-config")
        .get("default");
    startCoAPServer();

    startPromise.complete();
  }

  @Override
  public void stop(final Promise<Void> stopPromise) {
    this.server.stop();
    stopPromise.complete();
  }

  private void startCoAPServer() {
    CoapConfig.register();
    UdpConfig.register();
    this.server = new CoapServer();
    createRouter();
    this.server.start();
  }

  private void createRouter() {
    final CoapEntityHandler handler = new CoapEntityHandler(vertx, httpConfig);
    // workspaces directory as resource, needed explicitly to enable wildcards
    CoapResource workspaces = new Workspaces(handler);
    this.server.add(workspaces);



  }

  static class WorkspaceHandlerResource extends CoapResource {
    CoapEntityHandler handler;

    public WorkspaceHandlerResource(CoapEntityHandler handler, String name) {
      super(name, true);
      this.handler = handler;
      // find out if workspace exists
    }

    @Override
    public void handleGET(final CoapExchange exchange) {
      // on first get find out if workspace exists
      handler.handleGetEntity(this, exchange);
    }
  }

  static class Workspaces extends CoapResource {
    private final CoapEntityHandler handler;

    public Workspaces(CoapEntityHandler handler) {
      super("workspaces");
      this.handler = handler;
      getAttributes().setTitle("workspaces");
      getAttributes().addContentType(TEXT_PLAIN);
      getAttributes().addContentType(APPLICATION_JSON);
    }

    @Override
    public void handleGET(final CoapExchange exchange) {
      // get request to read out details
      exchange.accept();
      // start asynchronous processing, passing the exchange to a result callback
      Response response = new Response(CONTENT);
      response.setPayload("workspaces");
      exchange.respond(response);
    }

    @Override
    public Resource getChild(String name) {
      final var children = getChildren();
      final var match = children.stream().filter(child -> child.getName().equals(name)).findFirst();
      if (match.isPresent()) {
        return match.get();
      }
      WorkspaceHandlerResource newWorkspace = new WorkspaceHandlerResource(handler, name);
      this.add(newWorkspace);
      return newWorkspace;
    }
  }
}
