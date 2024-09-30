package org.hyperagents.yggdrasil.coap;

import static org.eclipse.californium.core.coap.CoAP.ResponseCode.CONTENT;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.Objects;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.hyperagents.yggdrasil.eventbus.messageboxes.CartagoMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.CartagoMessage;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.hyperagents.yggdrasil.utils.EnvironmentConfig;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;

/**
 * Handler for coAP requests.
 */
public class CoapEntityHandler {

  RdfStoreMessagebox rdfStoreMessagebox;
  CartagoMessagebox cartagoMessagebox;
  HttpInterfaceConfig httpConfig;

  /**
   * Default Constructor.
   */
  public CoapEntityHandler(final Vertx vertx, final HttpInterfaceConfig httpConfig, final
      EnvironmentConfig environmentConfig) {
    this.rdfStoreMessagebox = new RdfStoreMessagebox(vertx.eventBus());
    this.cartagoMessagebox = new CartagoMessagebox(
        vertx.eventBus(),
        environmentConfig
    );
    this.httpConfig = httpConfig;

  }

  /**
   * Returns the representation of the entity at the given Uri.
   */
  public void handleGetEntity(CoapExchange exchange) {
    final var uri =
        this.httpConfig.getBaseUriTrailingSlash() + exchange.getRequestOptions().getUriPathString();

    this.rdfStoreMessagebox
        .sendMessage(new RdfStoreMessage.GetEntity(uri)).onSuccess(
            s -> {
              exchange.accept();
              // start asynchronous processing, passing the exchange to a result callback
              Response response = new Response(CONTENT);
              response.setPayload(s.body());
              exchange.respond(response);
            }
        )
        .onFailure(
            f -> {
              exchange.accept();
              // start asynchronous processing, passing the exchange to a result callback
              Response response = new Response(CONTENT);
              response.setPayload(f.getMessage());
              exchange.respond(response);
            });
  }

  /**
   * Handles post requests to a specific workspace.
   */
  public void handlePostWorkspace(final CoapExchange exchange) {
    // check if it is trying to do /join or post to workspace
    final var uriPathString = exchange.getRequestOptions().getUriPathString().split("/");
    if (Objects.equals(uriPathString[uriPathString.length - 1], "join")) {
      final var requestPayload = exchange.getRequestText();
      JsonObject jsonObject = new JsonObject(requestPayload);


      final var agentId = jsonObject.getString("agentId");
      final var agentBodyName = jsonObject.getString("agentName");
      final var workspaceName = uriPathString[uriPathString.length - 2];

      this.cartagoMessagebox
          .sendMessage(new CartagoMessage.JoinWorkspace(
              agentId,
              agentBodyName,
              workspaceName
          ))
          .compose(response -> this.rdfStoreMessagebox
              .sendMessage(new RdfStoreMessage.CreateBody(
                  workspaceName,
                  agentId,
                  agentBodyName,
                  response.body()
              ))
          ).onSuccess(s -> {
            exchange.accept();
            // start asynchronous processing, passing the exchange to a result callback
            Response response = new Response(CONTENT);
            response.setPayload(s.body());
            exchange.respond(response);
          }).onFailure(f -> {
            exchange.accept();
            // start asynchronous processing, passing the exchange to a result callback
            Response response = new Response(CONTENT);
            response.setPayload(f.getMessage());
            exchange.respond(response);
          });
    } else {
      exchange.reject();
    }
  }

  /**
   * Handles the deletion of all entities.
   */
  public void handleDeleteEntity(CoapExchange exchange) {
    final var uri =
        this.httpConfig.getBaseUriTrailingSlash() + exchange.getRequestOptions().getUriPathString();
    System.out.println("deleting: " + uri);
  }
}
