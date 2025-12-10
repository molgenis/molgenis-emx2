const fs = require("fs");
const path = require("path");
const process = require("process");
const svgo = require("svgo");

const sourceDir = "./app/assets/icons";
const targetDir = "./app/components/global/icons";

function camelize(str) {
  return str
    .replace(/(?:^\w|[A-Z]|\b\w)/g, function (word, index) {
      return index === 0 ? word.toLowerCase() : word.toUpperCase();
    })
    .replace(/\s+/g, "");
}

function capitalizeFirstLetter(string) {
  return string.charAt(0).toUpperCase() + string.slice(1);
}

fs.rmdirSync(targetDir, { recursive: true });
fs.mkdir(targetDir, (err) => {
  if (err) {
    return console.error(err);
  }
  console.log("Icons folder successfully created!");
});

// Loop through all the files in the temp directory
fs.readdir(sourceDir, function (err, files) {
  if (err) {
    console.error("Could not list the directory.", err);
    process.exit(1);
  }

  files.forEach(function (file) {
    // Make one pass and make the file complete
    const sourceFile = path.join(sourceDir, file);

    fs.readFile(sourceFile, "utf8", (err, data) => {
      if (err) {
        console.error(err);
        return;
      }

      const optimizedSvg = svgo.optimize(data, {
        plugins: [
          {
            name: "removeDimensions",
          },
          {
            name: "removeAttrs",
            params: {
              attrs: ["*:fill:*"]
            }
          },
          {
            name: 'makeEverythingPink',
            // type: "perItem",
            fn: (ast, params, info) => {
              return {
                element: {
                  enter: (node, parentNode) => {
                    delete node.attributes.viewBox
                    // node.attributes.fill = 'pink';
                  },
                },
              };
            },
            // fn: function(item) {
            //   console.log(item);
            //   // item.attributes["fill"] = "pink"
            // },
          }
        ]
      });

      let newFileName = camelize(file);
      newFileName = capitalizeFirstLetter(newFileName);
      newFileName = newFileName.replace("Svg", "vue");

      const toPath = path.join(targetDir, newFileName);

      const newFile = `<template>
  ${optimizedSvg.data} 
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