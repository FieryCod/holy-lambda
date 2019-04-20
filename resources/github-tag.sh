#!/usr/bin/env sh
VERSION=$(cat project.clj | egrep -o -m 1 '[0-9].[0-9].[0-9]')

if [[ $(git name-rev --name-only --tags HEAD) = "$VERSION" ]]; then
    echo "Already tagged. Skipping.."
    exit
fi

./node_modules/.bin/conventional-changelog -p angular -i CHANGELOG.md -s
git add CHANGELOG.md
git commit -q -m "chore(release): Version ${VERSION} :tada:

$(./node_modules/.bin/conventional-changelog -p angular)
"
git tag ${VERSION}

git push --tags
