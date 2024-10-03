package org.hyperagents.yggdrasil.coap;

import static org.eclipse.californium.core.coap.CoAP.ResponseCode.CONTENT;

import io.vertx.core.Vertx;
import java.util.List;
import java.util.Objects;
import org.eclipse.californium.core.coap.Option;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.hyperagents.yggdrasil.eventbus.messageboxes.CartagoMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.CartagoMessage;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.hyperagents.yggdrasil.utils.EnvironmentConfig;
import org.hyperagents.yggdrasil.utils.NetworkInterfaceConfig;

/**
 * Handler for coAP requests.
 */
public class CoapEntityHandler {

  private final RdfStoreMessagebox rdfStoreMessagebox;
  private final CartagoMessagebox cartagoMessagebox;
  private final NetworkInterfaceConfig coapConfig;
  private final NetworkInterfaceConfig httpConfig;

  /**
   * Default Constructor.
   */
  public CoapEntityHandler(final Vertx vertx,
                           final NetworkInterfaceConfig coapConfig,
                           final NetworkInterfaceConfig httpConfig,
                           final EnvironmentConfig environmentConfig) {
    this.rdfStoreMessagebox = new RdfStoreMessagebox(vertx.eventBus());
    this.cartagoMessagebox = new CartagoMessagebox(
        vertx.eventBus(),
        environmentConfig
    );
    this.coapConfig = coapConfig;
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
      final var metadata = exchange.getRequestText();

      OptionSet optionSet = exchange.advanced().getRequest().getOptions();
      List<Option> options = optionSet.asSortedList();

      final var agentId = options.stream().filter(o -> o.getNumber() == 500)
          .findFirst()
          .orElseThrow()
          .getStringValue();

      final var agentBodyName = options.stream().filter(o -> o.getNumber() == 600)
          .findFirst()
          .orElseThrow()
          .getStringValue();

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
          )
          .compose(r -> this.rdfStoreMessagebox
              .sendMessage(new RdfStoreMessage.UpdateEntity(
                  this.httpConfig.getAgentBodyUri(workspaceName, agentBodyName),
                  metadata
              )))
          .onSuccess(s -> {
            exchange.accept();
            Response response = new Response(CONTENT);
            response.setPayload(s.body());
            exchange.respond(response);
          }).onFailure(f -> {
            exchange.accept();
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
