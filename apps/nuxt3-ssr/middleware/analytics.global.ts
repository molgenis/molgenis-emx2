export default defineNuxtRouteMiddleware((to, from) => {
  const config = useRuntimeConfig();
  if (
    config.public.analyticsKey &&
    useCookie("mg_allow_analytics").value &&
    typeof _sz !== "undefined" &&
    process.client
  ) {
    // note that _sz is defined in the global scope by the site improve analytics script
    // _sz.push uses a image.aspx request to send data to site improve, passing the data as query parameters
    _sz.push([
      config.public.analyticsKey,
      {
        url: to.fullPath,
        ref: from.fullPath,
        title: to.name,
      },
    ]);
  }
});
