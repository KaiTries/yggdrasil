package org.hyperagents.yggdrasil.store.impl;

public class ShaclRules {

  public static String artifactTDRule = """
                                        @prefix sh: <http://www.w3.org/ns/shacl#> .
                                        @prefix td: <https://www.w3.org/2019/wot/td#> .
                                        @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
                                        @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                                        @prefix ex: <http://www.example.org/> .

                                        ex:ThingShape a sh:NodeShape ;
                                            sh:targetClass td:Thing ;
                                            sh:property [
                                            sh:path rdf:type;
                                            sh:in (
                                            <https://www.w3.org/2019/wot/td#Thing>
                                            <https://purl.org/hmas/Artifact>
                                            ) ;
                                            sh:message "The node must be a td:Thing." ;
                                            ] ;
                                            sh:property [
                                                sh:path sh:subject ;
                                                sh:nodeKind sh:IRI ;
                                                sh:pattern "^http://localhost:8080/workspaces/test/artifacts/c0/#artifact$" ;
                                                sh:message "The URI must be http://localhost:8080/workspaces/test/artifacts/c0/#artifact." ;
                                            ] ;
                                            sh:closed false ;
                                            sh:ignoredProperties ( rdf:type ) .
                                        """;
  public static String oneResourceProfileRule = """
                                                @prefix sh: <http://www.w3.org/ns/shacl#> .
                                                @prefix ex: <http://example.org/> .
                                                @prefix hmas: <https://purl.org/hmas/> .

                                                ex:SingleResourceProfileShape a sh:NodeShape ;
                                                    sh:targetClass hmas:ResourceProfile ;
                                                    sh:property [
                                                        sh:path rdf:type ;
                                                        sh:qualifiedValueShape [
                                                            sh:class hmas:ResourceProfile ;
                                                        ] ;
                                                        sh:qualifiedMaxCount 1 ;
                                                        sh:qualifiedMinCount 1 ;
                                                    ] .
                                                """;
}
