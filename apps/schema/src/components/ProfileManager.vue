<template>
  <div class="container-fluid">
    <MessageError v-if="error">error: {{ error }} {{ search }}</MessageError>
    <div class="row">
      <div class="col-4">
        <div class="sticky-top" style="max-height: 100vh; overflow: auto">
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
          <p v-if="selectedProfiles?.length > 0">
            selected profiles:
            <span
              class="badge mr-1"
              :style="{ 'background-color': color(profile) }"
              v-for="profile in selectedProfiles"
              @click="
                selectedProfiles.splice(selectedProfiles.indexOf(profile), 1)
              "
              >{{ profile }}</span
            >
          </p>
          <InputSearch v-model="search" />
          <ul class="list-group sticky-top">
            <li
              v-for="table in tables"
              class="list-group-item list-group-item-action"
              :class="{
                active:
                  table.selected || table.columns?.some((c) => c.selected),
              }"
              v-scroll-to="{
                el: '#' + (table.name ? table.name.replaceAll(' ', '_') : ''),
                offset: -50,
              }"
            >
              <h5>
                {{ table.name }}
                <sup v-if="table.profiles">
                  <span
                    class="badge mr-1"
                    :style="{ 'background-color': color(profile) }"
                    v-for="profile in table.profiles"
                  >
                    {{ profile }}
                  </span>
                </sup>
              </h5>
            </li>
          </ul>
        </div>
      </div>
      <div class="col-6">
        <div v-for="table in tables">
          <h2 :id="table.name.replaceAll(' ', '_')">{{ table.name }}</h2>
          <div>{{ table.description }}</div>
          <ul class="list-group">
            <li class="list-group-item nav-header">
              <ButtonAlt @click="selectAll(table.name)">select all</ButtonAlt>
              <ButtonAlt @click="deselectAll(table.name)"
                >deselect all
              </ButtonAlt>
            </li>
            <template v-for="column in table.columns">
              <li
                v-if="isVisible(column)"
                class="list-group-item list-group-item-action"
                :class="{ active: column.selected }"
                @click="
                  column.selected
                    ? (column.selected = null)
                    : (column.selected = true)
                "
              >
                <h5>
                  {{ column.name }} {{}}
                  <sup>
                    <span
                      class="badge mr-1"
                      :style="{ 'background-color': color(profile) }"
                      v-for="profile in column.profiles"
                      >{{ profile }}</span
                    >
                  </sup>
                  <div class="float-right">
                    <small class="align-right">
                      <span
                        ><template
                          v-for="(semantics, index) in column.semantics"
                          ><template v-if="index > 0">,</template
                          >{{ semantics }}</template
                        ></span
                      >
                    </small>
                  </div>
                </h5>

                {{ column.description }}
                <div>
                  <small>
                    <i>{{
                      Object.keys(column)
                        .filter((key) =>
                          ["key", "refTableName", "columnType"].includes(key)
                        )
                        .map((key) => key + "=" + column[key])
                        .join(", ")
                    }}</i>
                  </small>
                </div>
              </li>
            </template>
          </ul>
        </div>
      </div>
      <div class="col-2">
        <h2>My selection:</h2>
        <a href="" @click.prevent="downloadCsv()">download</a>
        <ul>
          <li v-for="table in selectedTables">
            {{ table.name }}
            <ul v-if="table.columns?.some((c) => c.selected)">
              <li v-for="column in table.columns?.filter((c) => c.selected)">
                {{ column.name }}
              </li>
            </ul>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<style scoped>
.list-group li:hover {
  cursor: pointer;
}
</style>

<script setup>
import { ref, computed } from "vue";
import { InputSearch, MessageError, ButtonAlt } from "molgenis-components";
import VueScrollTo from "vue-scrollto";

const error = ref("");
const profileJson = ref({});
const selectedProfiles = ref([]);
const search = defineModel();

fetch("../api/profiles")
  .then((response) => response.json())
  .then((data) => {
    profileJson.value = data;
    profileJson.value.tables.forEach(
      (t) => (t.columns = t.columns?.filter((c) => !c.inherited))
    );
  })
  .catch((err) => (error.value = err));

const tables = computed(() => {
  if (profileJson.value) {
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
  }
});

const selectedTables = computed(() => {
  if (profileJson.value) {
    return profileJson.value.tables?.filter(
      (t) => t.selected || t.columns?.some((c) => c.selected)
    );
  }
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

const isVisible = function (column) {
  return (
    (selectedProfiles?.value.length === 0 ||
      column.profiles?.some((p) => selectedProfiles?.value.includes(p))) &&
    (!search.value || JSON.stringify(column).includes(search.value))
  );
};

const formatValue = function (value) {
  if (value === null) {
    return "";
  } else if (Array.isArray(value)) {
    if (value.length > 1) {
      return `"${value.join(",")}"`;
    } else {
      return formatValue(value[0]);
    }
  } else {
    return value;
  }
};

const selectAll = function (tableName) {
  tables.value.forEach((t) => {
    if (t.name === tableName) {
      t.columns?.forEach((c) => {
        c.selected = true;
      });
    }
  });
};

const deselectAll = function (tableName) {
  tables.value.forEach((t) => {
    if (t.name === tableName) {
      t.columns?.forEach((c) => {
        c.selected = false;
      });
    }
  });
};

const convertToCsv = function () {
  var csv =
    "tableName,tableExtends,columnName,columnType,key,required,refSchema,refTable,refLink,refBack,validation,semantics,description,profiles";
  selectedTables.value.forEach((t) => {
    csv += `${formatValue(t.name)},${formatValue(
      t.inheritName
    )},,,,,,,,,${formatValue(t.semantics)},${formatValue(
      t.descrpition
    )},${formatValue(t.profiles)}\n`;
    t.columns
      ?.filter((c) => c.selected)
      .forEach((c) => {
        csv += `${formatValue(t.name)},,${formatValue(c.name)},${formatValue(
          c.columnType
        )},${formatValue(c.key)},${formatValue(c.required)},${formatValue(
          c.refSchema
        )},${formatValue(c.refTableName)},${formatValue(
          c.refLink
        )},${formatValue(c.validation)},${formatValue(
          c.semantics
        )},${formatValue(c.description)},${formatValue(c.profiles)}\n`;
      });
  });
  return csv;
};

const downloadCsv = function () {
  const text = convertToCsv();
  const element = document.createElement("a");
  element.setAttribute(
    "href",
    "data:text/plain;charset=utf-8," + encodeURIComponent(text)
  );
  element.setAttribute("download", "molgenis.csv");
  element.style.display = "none";
  document.body.appendChild(element);

  element.click();

  document.body.removeChild(element);
};

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
