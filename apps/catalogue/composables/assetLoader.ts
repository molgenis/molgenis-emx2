export default {
  load: async function (assetName: string) {
    const logos = import.meta.glob("../assets/logos/**/*.svg", {
      query: "?raw",
    });
    const match = logos[`../assets/logos/${assetName}.svg`];
    return match();
  },
};
