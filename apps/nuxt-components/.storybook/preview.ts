import type { Preview } from "@storybook/vue3";
// import { hash } from "../../nuxt3-ssr/.fingerprint.js";
// import(`../../nuxt3-ssr/public/_nuxt-styles/css/styles.${hash}.css`).then(() => {
//   console.log('loaded tailwind styles')
// });
const preview: Preview = {
  parameters: {
    actions: { argTypesRegex: "^on[A-Z].*" },
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/,
      },
    },
    backgrounds: {
      default: "blue",
      values: [
        {
          name: "white",
          value: "#fff",
        },
        {
          name: "gray",
          value: "#1D1D1D",
        },
        {
          name: "blue",
          value: "#017FFD",
        },
      ],
    },
  },

  
};

export default preview;
