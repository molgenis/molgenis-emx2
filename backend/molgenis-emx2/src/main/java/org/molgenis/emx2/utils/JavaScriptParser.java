package org.molgenis.emx2.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.oracle.js.parser.ErrorManager;
import com.oracle.js.parser.Parser;
import com.oracle.js.parser.ScriptEnvironment;
import com.oracle.js.parser.Source;
import com.oracle.js.parser.ir.FunctionNode;
import com.oracle.js.parser.ir.IdentNode;
import com.oracle.js.parser.ir.LexicalContext;
import com.oracle.js.parser.ir.Scope;
import com.oracle.js.parser.ir.visitor.NodeVisitor;
import java.util.HashSet;
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

  private JavaScriptParser() {
    // hide constructor
  }

  /**
   * Returns the names of the variables a script reads from its context, e.g. the columns an
   * expression depends on. Names the script declares itself are not returned.
   */
  public static Set<String> getReferencedVariables(String script) {
    if (script == null || script.isBlank()) {
      return Set.of();
    }

    return CACHE.get(script, JavaScriptParser::parseReferencedVariables);
  }

  private static Set<String> parseReferencedVariables(String script) {
    try {
      FunctionNode ast =
          new Parser(
                  ENVIRONMENT,
                  Source.sourceFor("expression", JavaScriptUtils.prepareScript(script)),
                  new ErrorManager.ThrowErrorManager())
              .parse();

      ReferencedVariablesVisitor visitor = new ReferencedVariablesVisitor();
      ast.accept(visitor);
      return visitor.getReferencedVariables();
    } catch (Exception exception) {
      LOGGER.debug("cannot parse script '{}', assuming it reads no variables", script, exception);
      return Set.of();
    }
  }

  /**
   * Collects every identifier that is read but not declared by the script itself. Whether an
   * identifier is declared is decided per scope, using the scope chain the parser built: a name
   * that is, say, the parameter of an arrow function only hides an outer variable of that same name
   * within that arrow function, not in the rest of the script.
   */
  private static final class ReferencedVariablesVisitor extends NodeVisitor<LexicalContext> {

    private final Set<String> referencedVariables = new HashSet<>();

    private ReferencedVariablesVisitor() {
      super(new LexicalContext());
    }

    @Override
    public boolean enterIdentNode(IdentNode identNode) {
      if (!identNode.isPropertyName() && !isDeclaredInScope(identNode.getName())) {
        referencedVariables.add(identNode.getName());
      }
      return true;
    }

    private boolean isDeclaredInScope(String name) {
      for (Scope scope = getLexicalContext().getCurrentScope();
          scope != null;
          scope = scope.getParent()) {
        if (scope.hasSymbol(name)) {
          return true;
        }
      }
      return false;
    }

    private Set<String> getReferencedVariables() {
      return Set.copyOf(referencedVariables);
    }
  }
}
