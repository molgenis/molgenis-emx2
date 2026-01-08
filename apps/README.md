# Developer notes

- We use pnpm
- Copy 'helloworld' to create a new app
- Then you can simply run 'pnpm run dev' inside your app folder, e.g. cd to 'helloworld' and run 'pnpm run dev'

We also have component library in molgenis-components, which can be linked into your app

- When making changes to molgenis-components you have to run 'pnpm run build' to get your changes available in your app also

We use nx to build the workspace. If you don't want to use gradle:

Install dependencies first (or when you change package.json files)

```
pnpm install
```


most of us use vscode for frontend coding
