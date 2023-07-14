#! /usr/bin/env node
var shell = require("shelljs");
const fs = require("fs");
const path = require("path");

var location = "./public/_nuxt-styles/css/";

fs.readdir(location, (err, files) => {
  if (err) throw err;

  for (const file of files) {
    if (file !== "main.css") {
      fs.unlink(path.join(location, file), (err) => {
        if (err) throw err;
      });
    }
  }
});

shell.exec(
  `tailwindcss -c ./tailwind.config.cjs -i ${location}main.css -o ${location}styles.css --minify`
);
shell.exec(
  `tailwindcss -c ./tailwind.config.umcg.cjs -i ${location}main.css -o ${location}styles.umcg.css --minify`
);
shell.exec("nuxt build");
