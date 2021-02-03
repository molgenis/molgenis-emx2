module.exports = {
  devServer: {
    proxy: {
      "/graphql$": {
        target: "https://emx2-catalogue.test.molgenis.org",
        ws: true,
        changeOrigin: true,
      },
    },
  },
}