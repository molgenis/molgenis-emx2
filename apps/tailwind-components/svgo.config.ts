import type { Config } from "svgo";

const config: Config = {
  plugins: [
    {
      name: "convertColors",
      params: {
        currentColor: true,
      },
    },
    {
      name: "removeDimensions",
      params: {
        currentColor: true,
      },
    },
  ],
};

export default config;
