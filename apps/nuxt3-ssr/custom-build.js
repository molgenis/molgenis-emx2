#! /usr/bin/env node
var shell = require("shelljs");
var location = "./public/_nuxt-styles/css/";
shell.exec(
  `tailwindcss -c ./tailwind.config.cjs -i ${location}main.css -o ${location}styles.css --minify`
);
shell.exec(
  `tailwindcss -c ./tailwind.config.umcg.cjs -i ${location}main.css -o ${location}styles.umcg.css --minify`
);
shell.exec("nuxt build");
