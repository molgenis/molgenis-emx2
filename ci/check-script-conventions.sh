#!/usr/bin/env bash
set -euo pipefail

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

nodeVersion=$(tr -d '[:space:]' <.nvmrc)
declaredNodeRange=$(sed -n 's/.*"node": *"\([^"]*\)".*/\1/p' apps/package.json)
if [ ">=$nodeVersion" != "$declaredNodeRange" ]; then
  echo "apps/package.json engines.node is $declaredNodeRange but .nvmrc says $nodeVersion, expected >=$nodeVersion"
  status=1
fi

imageNodeVersion=$(sed -n 's/^ENV NODE_VERSION=//p' ci-build-images/Dockerfile)
if [ "$nodeVersion" != "$imageNodeVersion" ]; then
  echo "ci-build-images/Dockerfile NODE_VERSION is $imageNodeVersion but .nvmrc says $nodeVersion"
  status=1
fi

if [ "$status" -eq 0 ]; then
  echo "script conventions: no .mjs, no .cjs, node $nodeVersion in .nvmrc, apps/package.json and the ci image"
fi
exit $status
