package org.hyperagents.yggdrasil.store.impl;

import java.io.File;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.hyperagents.yggdrasil.store.RdfStore;

/**
 * Factory class to get an RDFStore.
 */
public final class RdfStoreFactory {

  private RdfStoreFactory() {}

  public static RdfStore createInMemoryStore() {
    ShaclSail shaclSail = new ShaclSail(new MemoryStore());
    return new Rdf4jStore(shaclSail);
  }

  public static RdfStore createFilesystemStore(final String storePath) {
    return new Rdf4jStore(new NativeStore(new File(storePath)));
  }
}
