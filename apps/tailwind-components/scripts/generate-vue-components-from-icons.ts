import fs from "node:fs";
import path from "node:path";
import process from "node:process";

const moveFrom = "./app/assets/minified-icons";
const moveTo = "./app/components/global/icons";

function camelize(str: string): string {
  return str
    .replace(/(?:^\w|[A-Z]|\b\w)/g, function (word, index) {
      return index === 0 ? word.toLowerCase() : word.toUpperCase();
    })
    .replace(/\s+/g, "");
}

function capitalizeFirstLetter(string: string): string {
  return string.charAt(0).toUpperCase() + string.slice(1);
}

fs.rmSync(moveTo, { recursive: true });
fs.mkdir(moveTo, (err) => {
  if (err) {
    return console.error(err);
  }
  console.log("Icons folder successfully created!");
});

// Loop through all the files in the temp directory
fs.readdir(moveFrom, function (err, files) {
  if (err) {
    console.error("Could not list the directory.", err);
    process.exit(1);
  }

  files.forEach(function (file) {
    // Make one pass and make the file complete
    const fromPath = path.join(moveFrom, file);

    fs.readFile(fromPath, "utf8", (err, data) => {
      if (err) {
        console.error(err);
        return;
      }

      let newFileName = camelize(file);
      newFileName = capitalizeFirstLetter(newFileName);
      newFileName = newFileName.replace("Svg", "vue");

      const toPath = path.join(moveTo, newFileName);

      const newFile = `<template>
  ${data} 
</template>`;

      fs.writeFile(toPath, newFile, { flag: "w" }, (err) => {
        if (err) {
          console.error(err);
          return;
        }
        //file written successfully
      });
    });
  });
});
