# Developer notes

- We use npm workspaces
- Copy 'helloworld' to create a new app
- At first run 'npm install --workspaces' in 'apps', this will download all dependencies
- Then you can simply run 'npm run dev' inside your app folder, e.g. cd to 'helloworld' and run 'npm run dev'

We also have component library in molgenis-components, which can be linked into your app

- When making changes to molgenis-components you have to run 'npm run build' to get your changes available in your app also

We use nx to build the workspace. If you don't want to use gradle:

Install dependencies first (or when you change package.json files)

```
npm install
```

build:

```
npm install --workspaces
```

most of us use vscode for frontend coding
