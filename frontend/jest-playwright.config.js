// https://github.com/playwright-community/jest-playwright/#configuration
module.exports = {
  browsers: ["chromium"], //, "firefox", "webkit"],
  verbose: true,
  serverOptions: {
    command: "yarn serve",
    port: 9092,
    launchTimeout: 10000,
    debug: true,
    options: {
      env: {
        E2E_TESTS: "true",
      },
    },
  },
  moduleNameMapper: {
    "^@/(.*)$": "<rootDir>/src/$1",
  },
  extraGlobals: [],
  collectCoverage: true,
  coverageReporters: ["text", "html"],
  mapCoverage: true,
  snapshotSerializers: ["jest-serializer-vue"],
  extraGlobals: [],
};
