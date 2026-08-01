const fs = require("fs");
const { writeGeneratedDoc } = require("./docs-generator");

module.exports = function () {
  let isDevServer = false;

  const configResolved = (resolvedConfig) => {
    isDevServer = resolvedConfig.command === "serve";
  };

  const transform = (code, id) => {
    if (/<docs>/.test(code)) {
      if (isDevServer) {
        writeGeneratedDoc(id, code);
      }

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

  return { name: "docs-plugin", configResolved, buildStart, transform };
};
