module.exports = function () {

const transform = (code, id) => {
    if (/<docs>/.test(code)) {
      console.log('remove docs from: ' + id);
      // console.log(code.replace(/<docs>[\s\S.]*<\/docs>/g, ""));
      return code.replace(/<docs>[\s\S.]*<\/docs>/g, "");
    }
}

  return { name: "docs-plugin", transform };

};
