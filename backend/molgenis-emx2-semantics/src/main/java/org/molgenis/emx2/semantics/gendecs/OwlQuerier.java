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
  private static final String owlFile = "data/gendecs/hp.owl";

  /**
   * Method that queries the hp.owl for all subclasses of an HPO term.
   *
   * @param hpoID String with the id of the HPO term which the user has entered
   * @return If the query is executed returns ResultSet with the results returns null if failed
   */
  private static ResultSet querySubClasses(String hpoID, Model model) {
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
  private static ResultSet queryParentClass(String hpoID, Model model) {
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

  public static ArrayList<String> getParentClasses(String hpoID) {
    Model model = FileManager.getInternal().loadModelInternal(owlFile);
    ResultSet resultSet = queryParentClass(hpoID, model);
    ArrayList<String> parents = new ArrayList<>();

    if (resultSet != null) {
      logger.debug("result variables: {}", resultSet.getResultVars());
      if (resultSet.hasNext()) {
        logger.debug("results found!");
        while (resultSet.hasNext()) {
          QuerySolution querySolution = resultSet.nextSolution();

          Resource resource = querySolution.getResource("predicate");
          if (resource.getLocalName().equals("subClassOf")) {
            // get the id of the term
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

  public static ArrayList<String> getSubClasses(String hpoID) {
    Model model = FileManager.getInternal().loadModelInternal(owlFile);
    ResultSet resultSet = querySubClasses(hpoID, model);
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
