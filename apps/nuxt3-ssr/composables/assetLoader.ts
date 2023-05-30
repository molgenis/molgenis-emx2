export default {
  load: async function (assetName: string) {
    const logos = import.meta.glob("../assets/logos/**/*.svg", { as: "raw" });
    const match = logos[`../assets/logos/${assetName}.svg`];
    return match();
  },
};
