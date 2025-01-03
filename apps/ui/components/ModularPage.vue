<template>
  <Page>
    <div v-if="content?.version === 2" >
      <ModulesModule
        v-for="(module, index) in localContent?.modules"
        @save="save($event, index)"
        @action="action($event, index)"
        :content="module"
        :editMode="editMode"
        :page="page"
      />
    </div>
    <div v-else v-html="localContent"></div>
  </Page>
</template>

<script setup lang="ts">
import { Page } from "molgenis-viz";
import { ref, watch } from "vue";

let props = withDefaults(
  defineProps<{
    content?: { modules: any[] };
    editMode?: boolean;
    page: string;
  }>(),
  {
    editMode: false,
  }
);

const emit = defineEmits();

let localContent = ref(props.content);
console.log("localContent",localContent.value);
function save(value, index) {
  console.log("save", value, index);
  emit("save", localContent.value);
}

function action(value, index) {
  console.log("action", value, index);
  const from = Math.max( 0, Math.min(index, localContent.value?.modules.length) );
  let to = 0;
  switch(value){
    case "up":
      if(from>0){
        to = from -1;
        var element = localContent.value?.modules[from];
        localContent.value?.modules.splice(from, 1);
        localContent.value?.modules.splice(to, 0, element);
      }
      break;
    case "down":
      to = from +1;
      var element = localContent.value?.modules[from];
      localContent.value?.modules.splice(from, 1);
      localContent.value?.modules.splice(to, 0, element);
      break;
    case "add":
      localContent.value?.modules.splice(from,0,{type:"Section", html:'', title:''});
      console.log("localContent",localContent.value);

      break;
    case "delete":
      if(localContent.value?.modules.length>1){
        localContent.value?.modules.splice(from,1);
      }   
      break;
  }
//  emit("action", value, index);
}

watch(
  () => props.content,
  (newValue) => {
    localContent.value = newValue;
  }
);
</script>
