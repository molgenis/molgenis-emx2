import { provider, Trigger } from "../types/Trigger";

function setupAnalytics(schemaName: string, providers: provider[]) {
  fetch(`/${schemaName}/api/trigger`)
    .then((response) => {
      response.json().then((data) => {
        data.forEach((trigger: Trigger) => {
          const elements = document.querySelectorAll(trigger.cssSelector);
          elements.forEach((element) => {
            console.log(`Setting up trigger for ${trigger.name}`);
            element.addEventListener("click", (e) => {
              for (let provider of providers) {
                handleEvent(e, trigger, element, provider);
              }
            });
          });
        });
      });
    })
    .catch((error) => {
      console.error("Error:", error);
    });
}

function handleEvent(
  event: Event,
  trigger: Trigger,
  element: Element,
  provider: provider
) {
  console.log("Event triggered", event, trigger, element.tagName, provider);
}

export { setupAnalytics };
