const fs = require("fs");

module.exports = function () {
  const transform = (code, id) => {
    if (/<docs>/.test(code)) {
      const findDocsElementRegExp = /<docs>[\s\S.]*<\/docs>/;

      // Save the docs in memory
      const docs = code.match(findDocsElementRegExp)[0];

      // Build a path to store the docs
      const pathSections = id.split("/");
      const componentsPathIndex = pathSections.findIndex((section) => {
        return section === "components";
      });
      const componentsPathSections = pathSections.splice(componentsPathIndex);
      const componentFileName = componentsPathSections.pop();
      const docPath = "./gen-docs/" + componentsPathSections.join("/");

      // Construct the folder tree for the docs components
      fs.mkdir(docPath, { recursive: true }, (err) => {
        if (err) throw err;

        const docTemplate = docs.replace(/(<docs>|<\/docs>)/g, "");

        // write out the docs to .vue files (needs to be done after folder was created)
        fs.writeFile(docPath + "/" + componentFileName, docTemplate, (err) => {
          if (err) {
            console.error(err);
            return;
          }
        });
      });

      // Strip the docs from the original vue sfc
      return code.replace(/<docs>[\s\S.]*<\/docs>/g, "");
    }
  };

  const buildStart = () => {
    if (process.env.CLEAR_GEN_FOLDERS === "on") {
      console.log("CLEAR_GEN_FOLDERS is set to 'on', clearing folders");
      fs.rmSync("./showCase/", { recursive: true, force: true });
      fs.rmSync("./gen-docs/", { recursive: true, force: true });
    }
  };

  return { name: "docs-plugin", buildStart, transform };
};
