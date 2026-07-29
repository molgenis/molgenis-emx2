#!/usr/bin/env bash
set -euo pipefail

allowedCommonJs="apps/directory/.eslintrc.cjs
apps/tailwind-components/scripts/create_vue_components_from_icons.cjs
apps/tailwind-components/svgo.config.cjs"

usageExitCode=2
status=0

esModuleScripts=$(git ls-files '*.mjs')
if [ -n "$esModuleScripts" ]; then
  echo "these must be TypeScript run by node, not .mjs:"
  echo "$esModuleScripts"
  status=1
fi

commonJsScripts=$(git ls-files '*.cjs')
unexpectedCommonJs=$(comm -23 <(echo "$commonJsScripts" | sort) <(echo "$allowedCommonJs" | sort))
if [ -n "$unexpectedCommonJs" ]; then
  echo "these must be TypeScript run by node, or added to the allowed list with the tool that requires CommonJS:"
  echo "$unexpectedCommonJs"
  status=1
fi

callerSources=$(git ls-files '*package.json' '.circleci/config.yml' '*.gradle')

isInvokedDirectly() {
  grep -qE "(node|bash) [^ \"']*$(basename "$1")( |\"|'|$)" $callerSources
}

canFailTheBuild() {
  grep -ohE '^[[:space:]]*exit +[^ ;)]+|process\.exit\([^)]*\)' "$1" |
    sed -E 's/.*exit *\(?//; s/\)$//' |
    tr -d ' ' |
    grep -qvE "^(0|$usageExitCode)$"
}

isNamedAsGate() {
  case "$(basename "$1")" in
  check-* | assert-*) return 0 ;;
  *) return 1 ;;
  esac
}

for script in $(git ls-files 'ci/*.sh' 'ci/*.ts' '*/scripts/*.sh' '*/scripts/*.ts'); do
  if isInvokedDirectly "$script" && canFailTheBuild "$script" && ! isNamedAsGate "$script"; then
    echo "$script is invoked directly and can fail the build, so it must be named check-* or assert-*"
    status=1
  fi
  if isNamedAsGate "$script" && ! canFailTheBuild "$script"; then
    echo "$script is named as a gate but has no reachable non-zero exit"
    status=1
  fi
done

nodeVersion=$(tr -d '[:space:]' <.nvmrc)
declaredNodeRange=$(sed -n 's/.*"node": *"\([^"]*\)".*/\1/p' apps/package.json)
if [ ">=$nodeVersion" != "$declaredNodeRange" ]; then
  echo "apps/package.json engines.node is $declaredNodeRange but .nvmrc says $nodeVersion, expected >=$nodeVersion"
  status=1
fi

if [ "$status" -eq 0 ]; then
  echo "script conventions: no .mjs, $(echo "$commonJsScripts" | grep -c .) allowed .cjs, gate names match gate behaviour, node $nodeVersion"
fi
exit $status
