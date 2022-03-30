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

  public OwlQuerier(String id) {
    hpoID = id;
  }

  /**
   * Method that queries the hp.owl for all subclasses of an HPO term.
   *
   * @param hpoID String with the id of the HPO term which the user has entered
   * @return If the query is executed returns ResultSet with the results returns null if failed
   */
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

  /**
   * Method that queries the hp.owl file for the parent class of an HPO term
   *
   * @param hpoID String with the id of the HPO term which the user has entered
   * @return If the query is executed returns ResultSet with the results returns null if failed
   */
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

  public ArrayList<String> getParentClasses() {
    model = FileManager.getInternal().loadModelInternal("data/gendecs/hp.owl");
    ResultSet resultSet = queryParentClass(hpoID);
    ArrayList<String> parents = new ArrayList<>();

    if (resultSet != null) {
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
    } else {
      logger.debug("result variables are null");
    }
    return parents;
  }

  public ArrayList<String> getSubClasses() {
    model = FileManager.getInternal().loadModelInternal("data/gendecs/hp.owl");
    ResultSet resultSet = querySubClasses(hpoID);
    ArrayList<String> hpoTerms = new ArrayList<>();
    if (resultSet != null) {
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
    } else {
      logger.debug("resultSet is null");
    }
    return hpoTerms;
  }
}
