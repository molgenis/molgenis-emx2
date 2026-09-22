export default {
  load: async function (assetName: string) {
    const logos = import.meta.glob("../assets/logos/**/*.svg", {
      query: "?raw",
    });
    const match = logos[`../assets/logos/${assetName}.svg`];
    if (typeof match === "function") {
      return match();
    } else {
      throw new Error(`Asset not found: ${assetName}`);
    }
  },
};
