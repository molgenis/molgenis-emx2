<script setup lang="ts">
import { Ref, ref } from "vue";
import { setupAnalytics } from "./lib/analytics";
import { Trigger } from "./types/Trigger";

const analyticsKey = ref("1234");

// setupAnalytics("catalogue", [{ id: "site-improve", options: { analyticsKey: analyticsKey.value } }]);

const schemaName = ref("catalogue");

const triggerName = ref("");
const cssSelector = ref("");

const triggers: Ref<Trigger[]> = ref([]);

function fetchTriggers() {
  fetch(`/catalogue/api/trigger`).then(async (response) => {
    triggers.value = await response.json();
  });
}

fetchTriggers();

const addTriggerError = ref("");

function addTrigger() {
  if (triggerName.value && cssSelector.value) {
    fetch(`/${schemaName.value}/api/trigger`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        name: triggerName.value,
        cssSelector: cssSelector.value,
      }),
    })
      .then((response) => response.json())
      .then((data) => {
        if (data.errors) {
          addTriggerError.value = data.errors;
          return;
        } else fetchTriggers();
      })
      .catch((error) => {
        console.error("Error:", error);
        addTriggerError.value = error;
      });
  }
}

function testBtnClicked() {
  console.log("Test button clicked");
}

function reRunSetup() {
  console.log("reRunSetup");
  setupAnalytics("catalogue", [
    { id: "site-improve", options: { analyticsKey: analyticsKey.value } },
  ]);
}
</script>

<template>
  <h1>Analytics</h1>

  <div id="page">
    <div class="container">
      <h2>Config</h2>
      <label for="id">Analytics key</label>
      <input id="id" v-model="analyticsKey" placeholder="event identifier" />

      <button @click="reRunSetup">(re) run setup</button>
      {{ analyticsKey }}
    </div>
    <div class="container">
      <h2>Add Trigger</h2>
      <label for="id">Event id</label>
      <input id="id" v-model="triggerName" placeholder="event identifier" />
      <label for="selector">Dom element css selector</label>
      <input
        id="selector"
        type="textarea"
        v-model="cssSelector"
        placeholder="css selector for event"
      />
      <button @click="addTrigger">Add Trigger</button>
      {{ addTriggerError }}
    </div>

    <div class="container">
      <h2>Triggers for {{ schemaName }}</h2>
      <ul>
        <li v-if="!triggers.length">No trigger setup</li>
        <li v-else v-for="trigger in triggers">
          <div>
            <dl>
              <dt>{{ trigger.name }}</dt>
              <dd>{{ trigger.cssSelector }}</dd>

              <dt v-if="trigger.appName">App</dt>
              <dd v-if="trigger.appName">{{ trigger.appName }}</dd>
            </dl>
          </div>
        </li>
      </ul>
    </div>

    <div>
      <button id="test-btn" @click="testBtnClicked">Test Button</button>
    </div>
  </div>
</template>

<style scoped>
input {
  margin: 0.5rem 0 1rem 0;
  padding: 0.3rem 2rem 0.3rem 0.3rem;
  font-size: x-large;
}

#page {
  display: flex;
  justify-content: space-between;
  padding: 1rem;
  width: 100%;

  #list-container {
    padding-right: 5rem;
  }

  p,
  dt {
    font-weight: bold;
  }

  dl,
  dd {
    text-align: left;
    font-size: 0.9rem;
  }

  dd {
    font-family: monospace;
    margin-bottom: 1em;
    padding-left: 0;
  }

  .container {
    display: flex;
    flex-direction: column;
    border: 1px solid black;
    padding: 0rem 1rem 1rem 1rem;

    label {
      font-size: x-large;
      text-align: left;
    }

    button {
      margin-top: 1rem;
      padding: 0.5rem 1rem;
      font-size: x-large;
      background-color: rgb(242, 185, 14);
    }
  }

  #test-btn {
    padding: 0.5rem 1rem;
    font-size: x-large;
    background-color: rgb(76, 72, 211);
    color: white;
  }
}
</style>
