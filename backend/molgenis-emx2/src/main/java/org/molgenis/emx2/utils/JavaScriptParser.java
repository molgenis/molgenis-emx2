package org.molgenis.emx2.utils;

import com.oracle.js.parser.ErrorManager;
import com.oracle.js.parser.Parser;
import com.oracle.js.parser.ScriptEnvironment;
import com.oracle.js.parser.Source;
import com.oracle.js.parser.ir.FunctionNode;
import com.oracle.js.parser.ir.IdentNode;
import com.oracle.js.parser.ir.LexicalContext;
import com.oracle.js.parser.ir.VarNode;
import com.oracle.js.parser.ir.visitor.NodeVisitor;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaScriptParser {

  private static final Logger LOGGER = LoggerFactory.getLogger(JavaScriptParser.class);

  private static final ScriptEnvironment ENVIRONMENT =
      ScriptEnvironment.builder().ecmaScriptVersion(ScriptEnvironment.ES_STAGING).build();

  private static final Map<String, Set<String>> CACHE = new ConcurrentHashMap<>();

  private static final int MAX_CACHE_SIZE = 10_000;

  private JavaScriptParser() {
    // hide constructor
  }

  public static Set<String> getReferencedVariables(String script) {
    if (script == null || script.isBlank()) {
      return Set.of();
    }

    Set<String> cached = CACHE.get(script);
    if (cached != null) {
      return cached;
    }

    Set<String> variables = parseReferencedVariables(script);
    if (CACHE.size() < MAX_CACHE_SIZE) {
      CACHE.put(script, variables);
    }

    return variables;
  }

  private static Set<String> parseReferencedVariables(String script) {
    Set<String> variables = new HashSet<>();
    Set<String> declared = new HashSet<>();
    try {
      FunctionNode ast =
          new Parser(
                  ENVIRONMENT,
                  Source.sourceFor("expression", JavaScriptUtils.prepareScript(script)),
                  new ErrorManager.ThrowErrorManager())
              .parse();

      ast.accept(
          new NodeVisitor<>(new LexicalContext()) {
            @Override
            public boolean enterIdentNode(IdentNode identNode) {
              if (!identNode.isPropertyName()) {
                variables.add(identNode.getName());
              }
              return true;
            }

            @Override
            public boolean enterFunctionNode(FunctionNode functionNode) {
              functionNode.getParameters().forEach(parameter -> declared.add(parameter.getName()));
              return true;
            }

            @Override
            public boolean enterVarNode(VarNode varNode) {
              declared.add(varNode.getName().getName());
              return true;
            }
          });
    } catch (Exception exception) {
      LOGGER.debug("cannot parse script '{}', assuming it reads no variables", script, exception);
      return Set.of();
    }

    variables.removeAll(declared);
    return Set.copyOf(variables);
  }
}
