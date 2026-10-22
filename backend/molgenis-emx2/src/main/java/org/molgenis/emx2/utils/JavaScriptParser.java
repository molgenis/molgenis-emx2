package org.molgenis.emx2.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.oracle.js.parser.ErrorManager;
import com.oracle.js.parser.Parser;
import com.oracle.js.parser.ScriptEnvironment;
import com.oracle.js.parser.Source;
import com.oracle.js.parser.ir.AccessNode;
import com.oracle.js.parser.ir.Expression;
import com.oracle.js.parser.ir.FunctionNode;
import com.oracle.js.parser.ir.IdentNode;
import com.oracle.js.parser.ir.IndexNode;
import com.oracle.js.parser.ir.LexicalContext;
import com.oracle.js.parser.ir.Scope;
import com.oracle.js.parser.ir.visitor.NodeVisitor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaScriptParser {

  private static final Logger LOGGER = LoggerFactory.getLogger(JavaScriptParser.class);

  private static final ScriptEnvironment ENVIRONMENT =
      ScriptEnvironment.builder().ecmaScriptVersion(ScriptEnvironment.ES_STAGING).build();

  private static final int MAX_CACHE_SIZE = 10_000;

  private static final Cache<String, Set<String>> CACHE =
      Caffeine.newBuilder().maximumSize(MAX_CACHE_SIZE).build();

  private static final Cache<String, Set<List<String>>> PATH_CACHE =
      Caffeine.newBuilder().maximumSize(MAX_CACHE_SIZE).build();

  private JavaScriptParser() {
    // hide constructor
  }

  public static Set<String> getReferencedVariables(String script) {
    if (script == null || script.isBlank()) {
      return Set.of();
    }

    return CACHE.get(script, JavaScriptParser::parseReferencedVariables);
  }

  public static Set<List<String>> getReferencedPaths(String script) {
    if (script == null || script.isBlank()) {
      return Set.of();
    }

    return PATH_CACHE.get(script, JavaScriptParser::parseReferencedPaths);
  }

  private static Set<String> parseReferencedVariables(String script) {
    try {
      ReferencedVariablesVisitor visitor = new ReferencedVariablesVisitor();
      parse(script).accept(visitor);
      return visitor.getReferencedVariables();
    } catch (Exception exception) {
      LOGGER.debug("cannot parse script '{}', assuming it reads no variables", script, exception);
      return Set.of();
    }
  }

  private static Set<List<String>> parseReferencedPaths(String script) {
    try {
      ReferencedPathsVisitor visitor = new ReferencedPathsVisitor();
      parse(script).accept(visitor);
      return visitor.getReferencedPaths();
    } catch (Exception exception) {
      LOGGER.debug("cannot parse script '{}', assuming it reads no paths", script, exception);
      return Set.of();
    }
  }

  private static FunctionNode parse(String script) {
    return new Parser(
            ENVIRONMENT,
            Source.sourceFor("expression", JavaScriptUtils.prepareScript(script)),
            new ErrorManager.ThrowErrorManager())
        .parse();
  }

  private abstract static class ScopeAwareVisitor extends NodeVisitor<LexicalContext> {

    private ScopeAwareVisitor() {
      super(new LexicalContext());
    }

    boolean isDeclaredInScope(String name) {
      for (Scope scope = getLexicalContext().getCurrentScope();
          scope != null;
          scope = scope.getParent()) {
        if (scope.hasSymbol(name)) {
          return true;
        }
      }
      return false;
    }
  }

  private static final class ReferencedVariablesVisitor extends ScopeAwareVisitor {

    private final Set<String> referencedVariables = new HashSet<>();

    @Override
    public boolean enterIdentNode(IdentNode identNode) {
      if (!identNode.isPropertyName() && !isDeclaredInScope(identNode.getName())) {
        referencedVariables.add(identNode.getName());
      }
      return true;
    }

    private Set<String> getReferencedVariables() {
      return Set.copyOf(referencedVariables);
    }
  }

  private static final class ReferencedPathsVisitor extends ScopeAwareVisitor {

    private final Set<List<String>> referencedPaths = new HashSet<>();

    @Override
    public boolean enterAccessNode(AccessNode accessNode) {
      List<String> path = toPath(accessNode);
      if (path != null) {
        referencedPaths.add(path);
      }
      return true;
    }

    private List<String> toPath(Expression expression) {
      if (expression instanceof IdentNode identNode) {
        return identNode.isPropertyName() || isDeclaredInScope(identNode.getName())
            ? null
            : List.of(identNode.getName());
      } else if (expression instanceof AccessNode accessNode) {
        List<String> basePath = toPath(accessNode.getBase());
        if (basePath == null) {
          return null;
        }
        List<String> path = new ArrayList<>(basePath);
        path.add(accessNode.getProperty());
        return List.copyOf(path);
      } else if (expression instanceof IndexNode indexNode) {
        return toPath(indexNode.getBase());
      }
      return null;
    }

    private Set<List<String>> getReferencedPaths() {
      return Set.copyOf(referencedPaths);
    }
  }
}
