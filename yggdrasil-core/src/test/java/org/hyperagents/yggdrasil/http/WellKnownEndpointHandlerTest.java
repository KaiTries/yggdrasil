package org.hyperagents.yggdrasil.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.utils.EnvironmentConfig;
import org.hyperagents.yggdrasil.utils.NetworkInterfaceConfig;
import org.hyperagents.yggdrasil.utils.WebSubConfig;
import org.hyperagents.yggdrasil.utils.impl.EnvironmentConfigImpl;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;
import org.hyperagents.yggdrasil.utils.impl.WebSubConfigImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test class for the well-known endpoint handler.
 */
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
@ExtendWith(VertxExtension.class)
public class WellKnownEndpointHandlerTest {
  private static final int TEST_PORT = 8080;
  private static final String TEST_HOST = "localhost";

  private WebClient client;

  public WellKnownEndpointHandlerTest() {
  }

  /**
   * setup method.
   *
   * @param vertx    vertx
   * @param ctx      ctx
   */
  @BeforeEach
  public void setUp(final Vertx vertx, final VertxTestContext ctx) {
    final String ontology = "td";

    this.client = WebClient.create(vertx);
    final var httpConfig = new HttpInterfaceConfigImpl(JsonObject.of());
    vertx.sharedData()
        .<String, NetworkInterfaceConfig>getLocalMap("http-config")
        .put("default", httpConfig);
    vertx.sharedData()
        .<String, EnvironmentConfig>getLocalMap("environment-config")
        .put("default",
            new EnvironmentConfigImpl(JsonObject.of(
                "environment-config",
                JsonObject.of("enabled", true, "ontology", ontology)
            )));
    final var notificationConfig = new WebSubConfigImpl(
        JsonObject.of(
            "notification-config",
            JsonObject.of("enabled", true)
        ),
        httpConfig
    );
    vertx.sharedData()
        .<String, WebSubConfig>getLocalMap("notification-config")
        .put("default", notificationConfig);
    new HttpNotificationDispatcherMessagebox(vertx.eventBus(), notificationConfig).init();
    vertx.deployVerticle(new HttpServerVerticle(), ctx.succeedingThenComplete());
  }

  @AfterEach
  public void tearDown(final Vertx vertx, final VertxTestContext ctx) {
    vertx.close(ctx.succeedingThenComplete());
    this.client.close();
  }

  @Test
  public void testGetWellKnownEndpoint(final VertxTestContext ctx) {
    this.client.get(TEST_PORT, TEST_HOST, "/.well-known/core")
        .send(ctx.succeeding(response -> ctx.verify(() -> {
          assertEquals(200, response.statusCode());
          assertEquals("application/ld+json", response.getHeader("Content-Type"));
          ctx.completeNow();
        })));
  }
}
