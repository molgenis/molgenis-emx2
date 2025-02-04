# Developer notes

- We use yarn workspaces
- Copy 'helloworld' to create a new app
- At first run 'yarn' in 'apps', this will download all dependencies
- Then you can simply run 'yarn dev' inside your app folder, e.g. cd to 'helloworld' and run 'yarn dev'

We also have component library in molgenis-components, which can be linked into your app

- When making changes to molgenis-components you have to run 'yarn build' to get your changes available in your app also

We use nx to build the workspace. If you don't want to use gradle:

Install dependencies first (or when you change package.json files)

```
yarn install
```

build:

```
yarn nx run-many --target=build
```

clean

```
yarn nx run-many --target=build
```

to reset build nx cache

```
yarn nx reset
```

to clear all node downloads

```
find . -name 'node_modules' -type d -prune -print -exec rm -rf '{}' \;
```

most of us use vscode for frontend coding
