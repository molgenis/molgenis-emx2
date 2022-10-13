import vue from "@vitejs/plugin-vue2";

export default {
  //relative path
  base: "",
  plugins: [vue()],
  server: {
    port: 9090,
    proxy: require("../dev-proxy.config"),
  },
};
