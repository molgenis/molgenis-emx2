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

var fingerPrint = Date.now().toString(36);
fs.writeFileSync(
  "./utils/fingerprint.js",
  "export const hash = '" + fingerPrint + "';"
);

shell.exec(
  `tailwindcss -c ./tailwind.config.cjs -i ${location}main.css -o ${location}styles.${fingerPrint}.css --minify`
);
shell.exec(
  `tailwindcss -c ./tailwind.config.umcg.cjs -i ${location}main.css -o ${location}styles.umcg.${fingerPrint}.css --minify`
);
shell.exec("nuxt build");
