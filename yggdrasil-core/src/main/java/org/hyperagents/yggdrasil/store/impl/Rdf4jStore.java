package org.hyperagents.yggdrasil.store.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.eclipse.rdf4j.common.exception.ValidationException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLBooleanJSONWriter;
import org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLResultsJSONWriter;
import org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLBooleanXMLWriter;
import org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLWriter;
import org.eclipse.rdf4j.query.resultio.text.BooleanTextWriter;
import org.eclipse.rdf4j.query.resultio.text.csv.SPARQLResultsCSVWriter;
import org.eclipse.rdf4j.query.resultio.text.tsv.SPARQLResultsTSVWriter;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.hyperagents.yggdrasil.store.RdfStore;
import org.hyperagents.yggdrasil.utils.RdfModelUtils;

/**
 * Provides access to the rdfstore.
 */
public class Rdf4jStore implements RdfStore {
  private final Repository repository;
  private final RepositoryConnection connection;

  Rdf4jStore(final Sail store) {
    this.repository = new SailRepository(store);
    this.repository.init();
    this.connection = this.repository.getConnection();
  }

  private IRI fixEntityIri(final IRI entityIri) {
    final String entityIriString = entityIri.toString();
    final String fixedIri = entityIriString.endsWith("/") ? entityIriString : entityIriString + "/";
    return RdfModelUtils.createIri(fixedIri);
  }

  @Override
  public void isShaclValid(final String shaclRule, final String modelRepresentation,
                           final String entityIri)
      throws IOException {
    ShaclSail shaclSail = new ShaclSail(new MemoryStore());
    shaclSail.setParallelValidation(false);
    shaclSail.setDashDataShapes(false);

    Repository validationRepository = new SailRepository(shaclSail);
    validationRepository.init();

    try (RepositoryConnection validationConnection = validationRepository.getConnection()) {
      final var fixedEntityIri = fixEntityIri(RdfModelUtils.createIri(entityIri));

      // Load SHACL shapes into the temporary repository
      try (StringReader shaclRules = new StringReader(ShaclRules.oneResourceProfileRule)) {
        validationConnection.begin();
        validationConnection.add(shaclRules, fixedEntityIri.stringValue(), RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
        validationConnection.commit();
      }

      // Load the model to be validated
      validationConnection.begin();
      final var model = RdfModelUtils.stringToModel(modelRepresentation, fixedEntityIri, RDFFormat.TURTLE);
      validationConnection.add(model, fixedEntityIri);

      // Attempt to commit, triggering SHACL validation
      try {
        validationConnection.commit();
      } catch (RepositoryException exception) {
        Throwable cause = exception.getCause();

        if (cause instanceof ValidationException) {
          Model validationReportModel = ((ValidationException) cause).validationReportAsModel();

          WriterConfig writerConfig = new WriterConfig()
              .set(BasicWriterSettings.INLINE_BLANK_NODES, true)
              .set(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL, true)
              .set(BasicWriterSettings.PRETTY_PRINT, true);

          Rio.write(validationReportModel, System.out, RDFFormat.TURTLE, writerConfig);
        }
        throw exception;
      }
    } finally {
      // Shut down the temporary repository after validation
      validationRepository.shutDown();
    }
  }

  @Override
  public boolean containsEntityModel(final IRI entityIri) throws IOException {
    final var fixedEntityIri = fixEntityIri(entityIri);

    try {
      return this.connection.hasStatement(
        null,
        null,
        null,
        false,
        fixedEntityIri
      );
    } catch (final RepositoryException e) {
      throw new IOException(e);
    }
  }

  @Override
  public Optional<Model> getEntityModel(final IRI entityIri) throws IOException {
    final var fixedEntityIri = fixEntityIri(entityIri);

    try {
      final Model model =
          QueryResults.asModel(
              this.connection.getStatements(null, null, null, fixedEntityIri));
      final var connectionNamespaces = new HashMap<String, Namespace>();

      for (final Namespace namespace : this.connection.getNamespaces()) {
        connectionNamespaces.put(namespace.getName(), namespace);
      }

      final var modelIris = RdfModelUtils.collectAllIriNamespaces(model);

      for (final String iri : modelIris) {
        if (connectionNamespaces.containsKey(iri)) {
          model.setNamespace(connectionNamespaces.get(iri));
        }
      }
      return Optional.of(model).filter(r -> !r.isEmpty());
    } catch (final RepositoryException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void addEntityModel(final IRI entityIri, final Model entityModel) throws IOException {
    final var fixedEntityIri = fixEntityIri(entityIri);

    try {
      this.connection.add(entityModel, fixedEntityIri);
      entityModel.getNamespaces().forEach(
          namespace -> this.connection.setNamespace(namespace.getPrefix(), namespace.getName()));
    } catch (final RepositoryException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void replaceEntityModel(final IRI entityIri, final Model entityModel) throws IOException {
    final var fixedEntityIri = fixEntityIri(entityIri);

    this.removeEntityModel(fixedEntityIri);
    this.addEntityModel(fixedEntityIri, entityModel);
  }

  @Override
  public void removeEntityModel(final IRI entityIri) throws IOException {
    final var fixedEntityIri = fixEntityIri(entityIri);

    try {
      this.connection.clear(fixedEntityIri);
    } catch (final RepositoryException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void close() throws IOException {
    try {
      this.connection.close();
      this.repository.shutDown();
    } catch (final RepositoryException e) {
      throw new IOException(e);
    }
  }

  @Override
  public String queryGraph(
      final String query,
      final List<String> defaultGraphUris,
      final List<String> namedGraphUris,
      final String responseContentType
  ) throws IllegalArgumentException, IOException {
    try (var out = new ByteArrayOutputStream()) {
      final var preparedQuery = this.connection.prepareQuery(query);
      final var originalQueryDataset =
          Optional.ofNullable(preparedQuery.getDataset()).orElse(new SimpleDataset());
      final var queryDataset = new SimpleDataset();
      originalQueryDataset
          .getDefaultRemoveGraphs()
          .forEach(queryDataset::addDefaultRemoveGraph);
      queryDataset.setDefaultInsertGraph(originalQueryDataset.getDefaultInsertGraph());
      if (!defaultGraphUris.isEmpty()) {
        defaultGraphUris.forEach(s -> queryDataset.addDefaultGraph(RdfModelUtils.createIri(s)));
      } else {
        originalQueryDataset.getDefaultGraphs().forEach(queryDataset::addDefaultGraph);
      }
      if (!namedGraphUris.isEmpty()) {
        namedGraphUris.forEach(s -> queryDataset.addNamedGraph(RdfModelUtils.createIri(s)));
      } else {
        originalQueryDataset.getNamedGraphs().forEach(queryDataset::addNamedGraph);
      }
      preparedQuery.setDataset(queryDataset);
      switch (preparedQuery) {
        case TupleQuery preparedTupleQuery -> preparedTupleQuery.evaluate(
                    responseContentType.equals("application/sparql-results+xml")
                            ? new SPARQLResultsXMLWriter(out)
                            : (responseContentType.equals("application/sparql-results+json")
                            ? new SPARQLResultsJSONWriter(out)
                            : (responseContentType.equals("text/tab-separated-values")
                            ? new SPARQLResultsTSVWriter(out)
                            : new SPARQLResultsCSVWriter(out)
                    )
                    )
            );
        case BooleanQuery preparedBooleanQuery -> (
                    responseContentType.equals("application/sparql-results+xml")
                            ? new SPARQLBooleanXMLWriter(out)
                            : (responseContentType.equals("application/sparql-results+json")
                            ? new SPARQLBooleanJSONWriter(out)
                            : new BooleanTextWriter(out)
                    )
            )
                    .handleBoolean(preparedBooleanQuery.evaluate());
        case GraphQuery preparedGraphQuery -> out.writeBytes(
                 RdfModelUtils.modelToString(QueryResults.asModel(preparedGraphQuery.evaluate()),
                                 RDFFormat.TURTLE, null)
                         .getBytes(StandardCharsets.UTF_8)
         );
        default -> {
        }
      }
      return out.toString(StandardCharsets.UTF_8);
    } catch (final MalformedQueryException e) {
      throw new IllegalArgumentException(e);
    } catch (final RepositoryException
                   | QueryEvaluationException
                   | TupleQueryResultHandlerException e) {
      throw new IOException(e);
    }
  }
}
