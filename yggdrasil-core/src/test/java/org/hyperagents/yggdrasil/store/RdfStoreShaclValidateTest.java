package org.hyperagents.yggdrasil.store;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.HttpNotificationDispatcherMessage;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.impl.EnvironmentConfigImpl;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;
import org.hyperagents.yggdrasil.utils.impl.WebSubConfigImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * testclass.
 */
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
@ExtendWith(VertxExtension.class)
public class RdfStoreShaclValidateTest {
  private static final String URIS_EQUAL_MESSAGE = "The URIs should be equal";

  private static final String ADDITIONAL_METADATA = """
                                                    @prefix test: <http://www.test.org/>.
                                                    <http://www.test.org/testSubject> <http://www.test.org/testPredicate> <http://www.test.org/testObject>.
                                                    """;

  private final BlockingQueue<HttpNotificationDispatcherMessage> notificationQueue;
  private RdfStoreMessagebox storeMessagebox;

  public RdfStoreShaclValidateTest() {
    this.notificationQueue = new LinkedBlockingQueue<>();
  }

  /**
   * setup method.
   *
   * @param vertx vertx
   * @param ctx ctx
   */
  @BeforeEach
  public void setUp(final Vertx vertx, final VertxTestContext ctx) {
    final var httpConfig = new HttpInterfaceConfigImpl(JsonObject.of());
    vertx.sharedData()
        .<String, HttpInterfaceConfig>getLocalMap("http-config")
        .put("default", httpConfig);
    vertx.sharedData()
        .getLocalMap("environment-config")
        .put("default",
            new EnvironmentConfigImpl(JsonObject.of(
                "environment-config",
                JsonObject.of(
                    "enabled",
                    true,
                    "ontology",
                    "td"
                )
            )));
    final var notificationConfig = new WebSubConfigImpl(
        JsonObject.of(
            "notification-config",
            JsonObject.of("enabled", true)
        ),
        httpConfig
    );
    vertx.sharedData()
        .getLocalMap("notification-config")
        .put("default", notificationConfig);
    this.storeMessagebox = new RdfStoreMessagebox(vertx.eventBus());
    final var notificationMessagebox = new HttpNotificationDispatcherMessagebox(
        vertx.eventBus(),
        notificationConfig
    );
    notificationMessagebox.init();
    notificationMessagebox.receiveMessages(m -> this.notificationQueue.add(m.body()));
    vertx.deployVerticle(new RdfStoreVerticle(), ctx.succeedingThenComplete());
  }

  @AfterEach
  public void tearDown(final Vertx vertx, final VertxTestContext ctx) {
    vertx.close(ctx.succeedingThenComplete());
  }


  @Test
  public void createValidArtifactWorksTD(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.CreateArtifact(
            "http://yggdrasil:8080/workspaces/w1/artifacts/c0",
            "c0",
            Files.readString(
                Path.of(ClassLoader.getSystemResource("c0_artifact_turtle_input.ttl").toURI()),
                StandardCharsets.UTF_8
            )
        ))
        .onComplete(ctx.succeedingThenComplete());

  }

}
