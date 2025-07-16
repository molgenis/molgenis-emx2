export default function trackMatomoEvent(
  category: string,
  action: string,
  name: string,
  value?: string | number | undefined
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
