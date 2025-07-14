export default function trackMatomoEvent(
  category: string,
  action: string,
  name: string,
  value?: any
) {
  if (window._paq) {
    if (value !== undefined) {
      window._paq.push(["trackEvent", category, action, name, value]);
    } else {
      window._paq.push(["trackEvent", category, action, name]);
    }
  } else {
    console.warn("Matomo tracking is not initialized.");
  }
}
