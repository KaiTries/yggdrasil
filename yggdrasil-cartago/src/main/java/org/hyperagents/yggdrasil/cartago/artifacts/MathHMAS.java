package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.hmas.interaction.shapes.IntegerSpecification;
import ch.unisg.ics.interactions.hmas.interaction.shapes.QualifiedValueSpecification;
import ch.unisg.ics.interactions.hmas.interaction.shapes.ValueSpecification;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.Random;


public class MathHMAS extends HypermediaHMASArtifact {
  @OPERATION
  public void egcd(final int a, final int b, final OpFeedbackParam<Integer> gcd, final OpFeedbackParam<Integer> x,final OpFeedbackParam<Integer> y) {
    this.log("Calculating egcd of " + a + " and " + b);
    var temp = extendedEuclidean(a,b);
    gcd.set(temp[0]);
    x.set(temp[1]);
    y.set(temp[2]);
    this.log("GCD: "+ gcd.get() +",x: " + x.get() + ",y: " + y.get());
  }

  @OPERATION
  public void rand(final OpFeedbackParam<Integer> randomInt) {
   Random rand = new Random();
   int randInt = rand.nextInt();
   System.out.println("Random int: " + randInt);
   randomInt.set(randInt);
  }
  @OPERATION
  public void rand2(final OpFeedbackParam<Integer> randInt1, final OpFeedbackParam<Integer> randInt2) {
    Random rand = new Random();
    int one = rand.nextInt();
    int two = rand.nextInt();
    System.out.println("one: " + one + " two: " + two);
    randInt1.set(one);
    randInt2.set(two);
  }
  @Override
  protected void registerInteractionAffordances() {
    this.registerSignifier(
      "http://example.org/egcd",
      "egcd",
      "egcd",
      new QualifiedValueSpecification.Builder()
        .setIRIAsString("http://example.org/egcds")
        .addRequiredSemanticType(RDF.LIST.stringValue())
        .setRequired(true)
        .addPropertySpecification(RDF.FIRST.stringValue(),
          new IntegerSpecification.Builder()
            .setName("1st Parameter")
            .setRequired(true)
            .build())
        .addPropertySpecification(RDF.REST.stringValue(),
          new QualifiedValueSpecification.Builder()
            .setIRIAsString("http://example.org/egcdsRest")
            .setRequired(true)
            .addRequiredSemanticType(RDF.LIST.stringValue())
            .addPropertySpecification(
              RDF.FIRST.stringValue(),
              new IntegerSpecification.Builder()
                .setName("2nd Parameter")
                .setRequired(true)
                .build()
            )
            .addPropertySpecification(
              RDF.REST.stringValue(),
              new ValueSpecification.Builder()
                .addRequiredSemanticType(RDF.LIST.stringValue())
                .setValueAsString(RDF.NIL.stringValue())
                .setRequired(true)
                .build()
            )
            .build())
        .build()
    );
    this.registerFeedbackParameters("egcd",3);
    this.registerSignifier(
      "http://example.org/rand",
      "rand",
      "rand"
    );
    this.registerFeedbackParameters("rand",1);
    this.registerSignifier(
      "http://example.org/rand2",
      "rand2",
      "rand2"
    );
    this.registerFeedbackParameters("rand2",2);
  }

  public static int[] extendedEuclidean(int a, int b) {
    if (b == 0) {
      return new int[] {a, 1, 0};
    } else {
      int[] arr = extendedEuclidean(b, a % b);
      int gcd = arr[0];
      int x = arr[2];
      int y = arr[1] - (a / b) * arr[2];
      return new int[] {gcd, x, y};
    }
  }
}
