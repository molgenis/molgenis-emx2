#! /usr/bin/env node
var shell = require("shelljs");
shell.exec(
  "tailwindcss -c ./tailwind.config.cjs -i ./public/css/main.css -o ./public/css/styles.css"
);
shell.exec(
  "tailwindcss -c ./tailwind.config.umcg.cjs -i ./public/css/main.css -o ./public/css/styles.umcg.css",
);
shell.exec("nuxt build");
