<template>
  <p>error: {{ error }} {{ search }}</p>
  <p>
    available profiles:
    <span
      class="badge mr-1"
      :style="{ 'background-color': color(profile) }"
      v-for="profile in profiles.filter(
        (profile) => !selectedProfiles.includes(profile)
      )"
      @click="selectedProfiles.push(profile)"
      >{{ profile }}</span
    >
  </p>
  <p>
    selected profiles:
    <span
      class="badge mr-1"
      :style="{ 'background-color': color(profile) }"
      v-for="profile in selectedProfiles"
      @click="selectedProfiles.splice(selectedProfiles.indexOf(profile), 1)"
      >{{ profile }}</span
    >
  </p>
  <InputSearch v-model="search" />
  <div class="row">
    <div class="col-3">
      <h2>Available tables</h2>
    </div>
    <div class="col-9" v-if="selectedTable">
      <h2>Selected table: {{ selectedTable.name }}</h2>
      <div>{{ selectedTable.description }}</div>
    </div>
  </div>
  <div class="row">
    <div class="col-3">
      <ul class="list-group">
        <li
          v-for="table in tables"
          class="list-group-item list-group-item-action"
          :class="{ active: table === selectedTable }"
          @click="selectedTable = table"
        >
          <h5>
            {{ table.name }}
            <sup v-if="table.profiles"
              ><span
                class="badge mr-1"
                :style="{ 'background-color': color(profile) }"
                v-for="profile in table.profiles"
                >{{ profile }}</span
              ></sup
            >
          </h5>
        </li>
      </ul>
    </div>
    <div class="col-9" v-if="selectedTable">
      <ul class="list-group">
        <li
          v-for="column in columns"
          class="list-group-item list-group-item-action"
        >
          <h5>
            {{ column.name }}
            <sup
              ><span
                class="badge mr-1"
                :style="{ 'background-color': color(profile) }"
                v-for="profile in column.profiles"
                >{{ profile }}</span
              ></sup
            >
            <div class="float-right">
              <small class="align-right"
                ><a
                  v-for="semantics in column.semantics"
                  href="semantics"
                  target="_blank"
                  >{{ semantics }}</a
                ></small
              >
            </div>
          </h5>

          {{ column.description }}
          <div>
            <small
              ><i>{{
                Object.keys(column)
                  .filter((key) =>
                    ["key", "refTableName", "columnType"].includes(key)
                  )
                  .map((key) => key + "=" + column[key])
                  .join(", ")
              }}</i></small
            >
          </div>
        </li>
      </ul>
    </div>
  </div>
  <pre>
  <p>{{ profileJson }}</p>
    </pre>
</template>

<script setup>
import { ref, computed } from "vue";
import { InputSearch } from "molgenis-components";

const error = ref(null);
const profileJson = ref({});
const selectedTable = ref(null);
const selectedProfiles = ref([]);
const search = defineModel();

fetch("../api/profiles")
  .then((response) => response.json())
  .then((data) => {
    profileJson.value = data;
    profileJson.value.tables.forEach(
      (t) => (t.columns = t.columns.filter((c) => !c.inherited))
    );
  })
  .catch((err) => (error.value = err));

const tables = computed(() => {
  return profileJson.value.tables?.filter(
    (t) =>
      ((!search.value || JSON.stringify(t).includes(search.value)) &&
        selectedProfiles.value.length == 0) ||
      selectedProfiles.value.some(
        (p) =>
          t.profiles?.includes(p) ||
          t.columns?.some((c) => c.profiles?.includes(p))
      )
  );
});

const profiles = computed(() => {
  const result = [];
  if (!profileJson.value.tables) {
    return [];
  }
  profileJson.value.tables.forEach((t) => {
    if (t.profiles) {
      result.push(...t.profiles);
    }
    if (t.columns) {
      t.columns.forEach((c) => {
        if (c.profiles) {
          result.push(...c.profiles);
        }
      });
    }
  });
  return Array.from(new Set(result));
});

const columns = computed(() => {
  if (selectedTable.value) {
    return profileJson.value.tables
      .find((t) => t === selectedTable.value)
      .columns.filter(
        (c) =>
          (selectedProfiles?.value.length == 0 ||
            c.profiles?.some((p) => selectedProfiles?.value.includes(p))) &&
          (!search.value || JSON.stringify(c).includes(search.value))
      );
  }
});

const color = function (profile) {
  var colorArray = [
    "#FF6633",
    "#FFB399",
    "#FF33FF",
    "#FFFF99",
    "#00B3E6",
    "#E6B333",
    "#3366E6",
    "#999966",
    "#99FF99",
    "#B34D4D",
    "#80B300",
    "#809900",
    "#E6B3B3",
    "#6680B3",
    "#66991A",
    "#FF99E6",
    "#CCFF1A",
    "#FF1A66",
    "#E6331A",
    "#33FFCC",
    "#66994D",
    "#B366CC",
    "#4D8000",
    "#B33300",
    "#CC80CC",
    "#66664D",
    "#991AFF",
    "#E666FF",
    "#4DB3FF",
    "#1AB399",
    "#E666B3",
    "#33991A",
    "#CC9999",
    "#B3B31A",
    "#00E680",
    "#4D8066",
    "#809980",
    "#E6FF80",
    "#1AFF33",
    "#999933",
    "#FF3380",
    "#CCCC00",
    "#66E64D",
    "#4D80CC",
    "#9900B3",
    "#E64D66",
    "#4DB380",
    "#FF4D4D",
    "#99E6E6",
    "#6666FF",
  ];
  return colorArray[profiles.value.indexOf(profile)];
};
</script>
