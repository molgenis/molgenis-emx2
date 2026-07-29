#!/usr/bin/env bash
set -euo pipefail

usageExitCode=2
status=0

esModuleScripts=$(git ls-files '*.mjs')
if [ -n "$esModuleScripts" ]; then
  echo "these must be TypeScript run by node, not .mjs:"
  echo "$esModuleScripts"
  status=1
fi

commonJsScripts=$(git ls-files '*.cjs')
if [ -n "$commonJsScripts" ]; then
  echo "these must be TypeScript run by node, not .cjs:"
  echo "$commonJsScripts"
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

nameAnnouncesItCanFail() {
  case "$(basename "$1")" in
  check-* | assert-* | generate-*) return 0 ;;
  *) return 1 ;;
  esac
}

for script in $(git ls-files 'ci/*.sh' 'ci/*.ts' '*/scripts/*.sh' '*/scripts/*.ts'); do
  if isInvokedDirectly "$script" && canFailTheBuild "$script" && ! nameAnnouncesItCanFail "$script"; then
    echo "$script is invoked directly and can fail the build, so it must be named check-*, assert-* or generate-*"
    status=1
  fi
  if nameAnnouncesItCanFail "$script" && ! canFailTheBuild "$script"; then
    echo "$script is named as if it can fail the build but has no reachable non-zero exit"
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
  echo "script conventions: no .mjs, no .cjs, names match failure behaviour, node $nodeVersion"
fi
exit $status
