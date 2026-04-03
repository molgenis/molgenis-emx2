#!/bin/bash
# get the last version
current_version="$(grep -w "version=" setup.py | cut -d'=' -f2 |tr -d \'\"\,)"
echo "CURRENT VERSION" $current_version

# if there is no version, use 0.0.0
if [ -z "$current_version" ]
then
  current_version="0.0.0"
fi

RELEASE_SCOPE=$1
echo "release scope is" $RELEASE_SCOPE

new_version="$(bumpversion --allow-dirty --current-version "$current_version" --list "$RELEASE_SCOPE" | grep new_version= | cut -d'=' -f2)"

echo "New version" "$new_version"
