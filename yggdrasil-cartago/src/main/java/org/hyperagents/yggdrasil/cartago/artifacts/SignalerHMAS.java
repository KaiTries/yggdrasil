package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.OPERATION;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;

/**
 * Example artifact that implements the signal functionality.
 */
@SuppressFBWarnings("PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_CLASS_NAMES")
public class SignalerHMAS extends HypermediaHMASArtifact {


  @OPERATION
  public void sign() {
    signal("tick");
  }

  @Override
  protected void registerInteractionAffordances() {
    // Register one action affordance with an input schema
    this.registerSignifier("http://example.org/Sign", "sign", "sign");
    final var model = new LinkedHashModel();
    this.addMetadata(model);
  }
}
