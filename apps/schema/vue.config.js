module.exports = {
  publicPath: '', // to ensure relative paths are used
  devServer: {
    port: '9090',
    proxy: {
      '^/api': { target: 'http://localhost:8080' }
    }
  }
}
