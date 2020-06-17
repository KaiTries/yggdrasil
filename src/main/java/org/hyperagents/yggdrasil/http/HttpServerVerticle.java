package org.hyperagents.yggdrasil.http;

import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.core.EventBusMessage;
import org.hyperagents.yggdrasil.core.EventBusMessage.Headers;
import org.hyperagents.yggdrasil.core.EventBusMessage.MessageType;
import org.hyperagents.yggdrasil.core.EventBusRegistry;

import com.google.common.net.HttpHeaders;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;


/** 
 * This verticle exposes an HTTP/1.1 interface for Yggdrasil. All requests are forwarded to a 
 * corresponding handler.
 */
public class HttpServerVerticle extends AbstractVerticle {

  public static final String DEFAULT_HOST = "0.0.0.0";
  public static final int DEFAULT_PORT = 8080;
  
  /* Keys used when parsing a JSON config file to extract HTTP settings */
  public static final String CONFIG_HTTP = "http-config";
  public static final String CONFIG_HTTP_PORT = "port";
  public static final String CONFIG_HTTP_HOST = "host";

  
  @Override
  public void start() {
    HttpServer server = vertx.createHttpServer();

    String host = DEFAULT_HOST;
    int port = DEFAULT_PORT;
    JsonObject httpConfig = config().getJsonObject(CONFIG_HTTP);

    if (httpConfig != null) {
      port = httpConfig.getInteger(CONFIG_HTTP_PORT, DEFAULT_PORT);
      host = httpConfig.getString(CONFIG_HTTP_HOST, DEFAULT_HOST);
    }

    Router router = createRouter();
    server.requestHandler(router::accept).listen(port, host);
  }

  /**
   * The HTTP API is defined here when creating the router.
   */
  private Router createRouter() {
    Router router = Router.router(vertx);
    
    router.route().handler(BodyHandler.create());
    
    router.get("/").handler((routingContext) -> {
      routingContext.response()
        .setStatusCode(HttpStatus.SC_OK)
        .end("Yggdrasil v0.0");
    });

    HttpEntityHandler handler = new HttpEntityHandler();
    HttpTemplateHandler templateHandler = new HttpTemplateHandler();

    router.get("/environments/:envid").handler(handler::handleGetEntity);
    router.post("/environments/").handler(handler::handleCreateEntity);
    router.put("/environments/:envid").handler(handler::handleUpdateEntity);
    router.delete("/environments/:envid").handler(handler::handleDeleteEntity);

    router.get("/workspaces/:wkspid").handler(handler::handleGetEntity);
    router.post("/workspaces/").handler(handler::handleCreateEntity);
    router.put("/workspaces/:wkspid").handler(handler::handleUpdateEntity);
    router.delete("/workspaces/:wkspid").handler(handler::handleDeleteEntity);

    router.get("/artifacts/templates").handler(templateHandler::handleGetTemplates);
    router.post("/artifacts/templates").handler(templateHandler::handleInstantiateTemp);
    router.get("/artifacts/templates/:classId").handler(templateHandler::handleGetClassDescription);

    router.get("/artifacts/:artid").handler(handler::handleGetEntity);
    router.post("/artifacts/").handler(handler::handleCreateEntity);
    router.put("/artifacts/:artid").handler(handler::handleUpdateEntity);
    router.delete("/artifacts/:artid").handler(handler::handleDeleteEntity);
    router.route("/artifacts/:artid/*").handler(handler::handleAction);
    
    // 2nd try to delete instantiated software artifact
//    router.delete("/artifacts/:artid").handler(templateHandler::handleDeleteInstance);
//    router.put("/artifacts/updateTriples/:artid").handler(templateHandler::handleUpdateTriples);
    // invoke actions on software artifacts defined in the annotations of the corresponding template
//    router.route("/artifacts/:artid/*").handler(templateHandler::handleAction);
//    router.route("/artifacts/:artid/*").consumes("application/json").handler(handler::handleAction);

    //route artifact manual requests
    router.get("/manuals/:wkspid").handler(handler::handleGetEntity);
    router.post("/manuals/").handler(handler::handleCreateEntity);
    router.put("/manuals/:wkspid").handler(handler::handleUpdateEntity);
    router.delete("/manuals/:wkspid").handler(handler::handleDeleteEntity);

    router.post("/hub/").handler(handler::handleEntitySubscription);

    // TODO: the following feature is added just for demo purposes
    router.post("/events/").handler(routingContext -> {
      String artifactIRI = routingContext.request().getHeader(HttpHeaders.LINK);

      Logger logger = LoggerFactory.getLogger(this.getClass().getName());
      logger.info("Got event for " + artifactIRI);

      EventBusMessage notification = new EventBusMessage(MessageType.ENTITY_CHANGED_NOTIFICATION)
                                            .setHeader(Headers.REQUEST_IRI, artifactIRI)
                                            .setPayload(routingContext.getBodyAsString());
      
      vertx.eventBus()
        .publish(EventBusRegistry.NOTIFICATION_DISPATCHER_BUS_ADDRESS, notification.toJson());

      routingContext.response().setStatusCode(HttpStatus.SC_OK).end();
    });
    
    return router;
  }
}
