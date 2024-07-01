function setupAnalytics(schemaName: string) {
  fetch(`/${schemaName}/api/trigger`)
    .then((response) => {
      response.json().then((data) => {
        data.forEach((trigger: any) => {
          const elements = document.querySelectorAll(trigger.cssSelector);
          elements.forEach((element) => {
            console.log(`Setting up trigger for ${trigger.name}`);
            element.addEventListener("click", () => {
              console.log(`Triggered ${trigger.name}`);
              alert(`Triggered ${trigger.name}`);
            });
          });
        });
      });
    })
    .catch((error) => {
      console.error("Error:", error);
    });
}

export { setupAnalytics };
