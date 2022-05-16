# Molgenis component library

This module helps developers create and share vue components for use with the molgenis EMX2 system

- minimal dependencies (gallery are excluded during production build )
- uses [Vite](https://vitejs.dev/) for project setup and fast hmr
- uses custom rollup plugin (docs-plugin.js) to extract samples / docs from components and use these in the component
  gallery (wip)

## Recommended IDE Setup

- [VSCode](https://code.visualstudio.com/)
  + [Volar](https://marketplace.visualstudio.com/items?itemName=johnsoncodehk.volar) Or
  [Intelij](https://www.jetbrains.com/idea/)

### Install deps

```yarn ```

### Run dev server to view component gallery

```yarn dev ```

### Build component library

```yarn build ```

### Build component library show case app

```yarn build-showcase ```

### Adding a new component

- start the dev server
- Add a vue sfc to the scr/components dir
- build and test your component
- add a sample to your sfc by using the ```<docs></docs>``` tag
- share component by adding it the lib/main.js export, and running the build
