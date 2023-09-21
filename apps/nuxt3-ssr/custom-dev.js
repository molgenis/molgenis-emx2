#! /usr/bin/env node
var shell = require("shelljs");
var location = "./public/_nuxt-styles/css/";
const fs = require("fs");
const path = require("path");

// cleanup public dir css folder
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

// write empty fingerprint file for development
fs.writeFileSync(
  ".fingerprint.js",
  "export const hash = '';"
);

shell.exec(
  `tailwindcss -c ./tailwind.config.cjs -i ${location}main.css -o ${location}styles.css --watch`,
  { async: true }
);
shell.exec(
  `tailwindcss -c ./tailwind.config.umcg.cjs -i ${location}main.css -o ${location}styles.umcg.css --watch`,
  { async: true }
);

shell.exec("nuxt dev");