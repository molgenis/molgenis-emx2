module.exports = {
  testEnvironment: "jsdom",
  transform: {
    "\\.[jt]sx?$": "babel-jest",
    "^.+\\.vue$": "@vue/vue3-jest",
  },
  testEnvironmentOptions: {
    customExportConditions: ["node", "node-addons"],
  },
  transformIgnorePatterns: ["/node_modules/"],
};
