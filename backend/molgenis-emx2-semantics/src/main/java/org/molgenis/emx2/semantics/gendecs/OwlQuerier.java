package org.molgenis.emx2.semantics.gendecs;

import java.util.ArrayList;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.FileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OwlQuerier {
  private static final Logger logger = LoggerFactory.getLogger(OwlQuerier.class);
  Model model;

  String hpoID;
  ArrayList<String> parents;
  ArrayList<String> subClasses;

  public OwlQuerier(String id) {
    hpoID = id;
  }

  private ResultSet querySubClasses(String hpoID) {
    String queryString =
        String.format(
            """
                        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
                        SELECT DISTINCT ?id ?label
                        WHERE {
                        ?id rdfs:subClassOf* <http://purl.obolibrary.org/obo/%s> .
                        ?id rdfs:label ?label
                        }
                """,
            hpoID);
    logger.debug("Querying for the sub classes of: {} with query: {}", hpoID, queryString);

    QueryFactory.create(queryString);
    try {
      QueryExecution queryExecution = QueryExecutionFactory.create(queryString, model);
      return queryExecution.execSelect();
    } catch (QueryException queryException) {
      queryException.printStackTrace();
    }
    return null;
  }

  private ResultSet queryParentClass(String hpoID) {
    String queryString =
        String.format(
            """
                        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
                        PREFIX owl: <http://www.w3.org/2002/07/owl#>
                        SELECT DISTINCT ?predicate ?label
                        WHERE {
                            <http://purl.obolibrary.org/obo/%s> ?predicate ?label .
                        }

                        """,
            hpoID);
    logger.debug("Querying for the parents of: {} with query: {}", hpoID, queryString);

    QueryFactory.create(queryString);
    try {
      QueryExecution queryExecution = QueryExecutionFactory.create(queryString, model);
      return queryExecution.execSelect();
    } catch (QueryException queryException) {
      queryException.printStackTrace();
    }
    return null;
  }

  private ArrayList<String> getParentsClasses(ResultSet resultSet) {
    ArrayList<String> parents = new ArrayList<>();
    logger.debug("result variables: {}", resultSet.getResultVars());
    if (resultSet.hasNext()) {
      logger.debug("results found!");
      while (resultSet.hasNext()) {
        QuerySolution querySolution = resultSet.nextSolution();

        Resource resource = querySolution.getResource("predicate");
        if (resource.getLocalName().equals("subClassOf")) {

          Resource literal = querySolution.getResource("label");
          parents.add(literal.getLocalName());
        }
      }
    } else {
      logger.debug("No results found");
    }
    return parents;
  }

  private ArrayList<String> getSubClasses(ResultSet resultSet) {
    ArrayList<String> hpoTerms = new ArrayList<>();
    if (resultSet.hasNext()) {
      logger.debug("results found!");
      while (resultSet.hasNext()) {
        QuerySolution querySolution = resultSet.nextSolution();

        Literal literal = querySolution.getLiteral("label");
        hpoTerms.add(literal.getString());
      }
    } else {
      logger.debug("No results found");
    }
    return hpoTerms;
  }

  public void executeQuery() {
    model = FileManager.getInternal().loadModelInternal("data/gendecs/hp.owl");
    logger.info("Loaded hp.owl");
    ResultSet resultsParents = queryParentClass(hpoID);
    ResultSet resultsSubClasses = querySubClasses(hpoID);
    logger.info("Queried hp.owl for parents and subclasses");

    parents = getParentsClasses(resultsParents);
    subClasses = getSubClasses(resultsSubClasses);

    logger.debug("Resulting parent array: {}", parents);
    logger.debug("Resulting sub classes array: {}", subClasses);
  }

  public ArrayList<String> getParents() {
    return parents;
  }

  public ArrayList<String> getSubClasses() {
    return subClasses;
  }
}
