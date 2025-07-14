export default function trackMatomoEvent(
  category: string,
  action: string,
  name: string
) {
  if (window._paq) {
    window._paq.push(["trackEvent", category, action, name]);
  } else {
    console.warn("Matomo tracking is not initialized.");
  }
}
