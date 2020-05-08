module.exports = {
  publicPath: '', // to ensure relative paths are used
  css: { extract: false },
  devServer: {
    port: '9090',
    proxy: {
      '^/graphql': {
        target: 'http://localhost:8080/api/graphql/pet%20store',
        pathRewrite: { '^/graphql': '' }
      }
    }
  }
}
