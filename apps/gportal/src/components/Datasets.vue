<template>
  <div>
    <RoutedTableExplorer
      tableName="Dataset"
      :showColumns="['id', 'title', 'description', 'publisher', 'distribution']"
      :canEdit="false"
      :canManage="false"
    >
      <template v-slot:rowheader="slotProps">
        <div class="checkbox">
          <input 
            :id="slotProps.row.id"
            type="checkbox"
            class="input"
            name="rems-selections"
            v-model="selection"
            :value="slotProps.row.id"
            :ref="setRefs"
          />
          <label :for="slotProps.row.id" class="label">
            <CheckCircleIcon />
            <span>Request</span>
          </label>
        </div>
      </template>
    </RoutedTableExplorer>
    <div class="d-flex flex-row justify-content-end">
      <ButtonAlt @click="clearAll">
        Clear all
      </ButtonAlt>
      <ButtonOutline @click="selectAll">
        Select all
      </ButtonOutline>
      <a :href="url" type="button" class="btn btn-primary mx-2">
        Request Access {{ selection.length ? `(${selection.length})` : ''}}
        <ShoppingCartIcon />
      </a>
    </div>
    <p>Your request:</p>
    <output class="d-block p-2 bg-dark text-light">
      <code>{{ selection }}</code><br/>
      <code>{{ url }}</code>
    </output>
  </div>
</template>

<script setup>
import { ref, watch } from "vue";
import { 
  RoutedTableExplorer,
  ButtonAlt,
  ButtonOutline
} from "molgenis-components";

import CheckCircleIcon from "./icons/check-circle.vue";
import ShoppingCartIcon from './icons/shopping-cart.vue';

let selection = ref([]);
let checkboxes = ref([]);
let url = ref('https://rems-gdi-nl.molgenis.net');

watch([selection], setUrl);

function setUrl() {
  const resources = selection.value.map(item => `resource=${item}`);
  url = `https://rems-gdi-nl.molgenis.net/apply-for?${resources.join('&')}`;
}

function clearAll () {
  selection.value = [];
  url = null;
}

function setRefs(value) {
  if (value !== null) {
    checkboxes.value.push(value._value);
  }
}

function selectAll () {
  checkboxes.value.forEach(value => {
    if (selection.value.indexOf(value) === -1) {
      selection.value.push(value);
    }
  });
  setUrl();
}

</script>

<style lang="scss">
.checkbox {
  position: relative;
  
  .label {
    display: flex;
    flex-direction: row;
    justify-content: flex-start;
    align-items: center;
    gap: 4px;
    margin: 0;
    width: 140px;
    padding: .375rem .75rem;
    background-color: hsl(210, 99%, 96%);
    border: 1px solid var(--blue);
    border-radius: .25rem;
    color: var(--blue);
    cursor: pointer;
    
    .heroicons {
      visibility: hidden;
      $size: 18px;
      height: $size;
      width: $size;
      margin-right: 8px;
    }
  }
 
  .input {
    position: absolute;
    clip: rect(1px 1px 1px 1px); /* IE6, IE7 */
    clip: rect(1px, 1px, 1px, 1px);
    overflow: hidden;
    height: 1px;
    width: 1px;
    margin: -1px;
    white-space: nowrap;
    
  }
  .input:checked + .label {
    .heroicons {
      visibility: visible;
    }
  }
}
</style>