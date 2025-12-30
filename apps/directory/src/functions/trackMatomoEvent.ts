import { IMatomoEvent } from "../interfaces/matomo";

export default function trackMatomoEvent(event: IMatomoEvent) {
  if (window._paq) {
    const eventToTrack: [string, string, string?, (string | number)?] = [
      "trackEvent",
      event.category,
      event.action,
    ];
    if (event.name) {
      eventToTrack.push(event.name);
    }
    if (event.value !== undefined) {
      eventToTrack.push(event.value);
    }
    window._paq.push(eventToTrack);
  } else {
    console.warn("Matomo tracking is not initialized.");
  }
}
