<template>
  <div v-for="shaclSet in shaclSets">
    <ShaclSetItem :id="shaclSet.name" :title="shaclSet.name">
      <table class="table">
        <tr>
        <th style="width:10%">name</th>
        <th style="width:30%">description</th>
        <th style="width:10%">version</th>
        <th>sources</th>
        </tr>
        <tr>
          <td>{{shaclSet.name}}</td>
          <td>{{shaclSet.description}}</td>
          <td>{{shaclSet.version}}</td>
          <td><ul><li v-for="source in shaclSet.sources"><a :href="source" target="_blank">{{source}}</a></li></ul></td>
        </tr>
      </table>
      <LayoutCard title="SHACL output" />
    </ShaclSetItem>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { parse } from "yaml";
import { TableDisplay, LayoutCard } from "molgenis-components";
import ShaclSetItem from "../components/ShaclSetItem.vue";

const shaclSets = ref("");
async function fetchShacls() {
  const res = await fetch("/api/rdf?shacls");
  shaclSets.value = await res.text();
  shaclSets.value = parse(shaclSets.value);
}

fetchShacls();
</script>