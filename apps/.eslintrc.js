module.exports = {
  root: true,

  env: {
    node: true,
    browser: true,
    es6: true,
    jest: true
  },

  extends: ['plugin:vue/essential', 'eslint:recommended'],

  parserOptions: {
    ecmaVersion: 2020
  },

  overrides: [
    {
      files: [
        '**/__tests__/*.{j,t}s?(x)',
        '**/tests/unit/**/*.spec.{j,t}s?(x)'
      ],
      env: {
        jest: true
      }
    }
  ]
};
