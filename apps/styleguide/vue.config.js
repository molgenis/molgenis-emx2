module.exports = {
  publicPath: '', // to ensure relative paths are used
  css: { extract: false },
  devServer: {
    port: '9090',
    proxy: {
      '^/api': { target: 'http://localhost:8080' }
    }
  }
}
