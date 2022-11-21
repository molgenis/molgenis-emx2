<script setup>
import { ref } from "vue";

const props = defineProps({
  title: {
    type: String,
  },
  json: {
    type: Object,
  },
  graphqlURL: {
    type: String
  }
});

let collapsedTitle = ref(true);
const toggleCollapseTitle = () => {
  collapsedTitle.value = !collapsedTitle.value;
};

const query = `
query 
AreasOfInformation( $filter:AreasOfInformationFilter, $orderby:AreasOfInformationorderby )
 {   
  AreasOfInformation( filter:$filter, limit:100000,  offset:0, orderby:$orderby )  
    {          
      order
      name 
      code 
      parent{ name }
      ontologyTermURI 
      definition 
      children{ name }
     }       
  AreasOfInformation_agg( filter:$filter ) { count }
  }
  `;

let resp = await $fetch(`/UMCG/catalogue/graphql`, {
  method: "POST",
  baseURL: 'http://localhost:3000/',
  body: {
    query,
  },
});
console.log('Search Filter data ')

let data = resp?.data?.AreasOfInformation
let count = resp?.data?.AreasOfInformation_agg?.count;

console.log(count)


// convert to tree of terms
//list all terms, incl subtrees
let terms = reactive({});
data.forEach((e) => {
  // did we see it maybe as parent before?
  if (terms[e.name]) {
    //then copy properties, currently only definition
    terms[e.name].definition = e.definition;
  } else {
    //else simply add the record
    terms[e.name] = {
      name: e.name,
      visible: true,
      selected: false,
      definition: e.definition,
    };
  }
  if (e.parent) {
    terms[e.name].parent = e.parent;
    //did we see this parent before?
    if (!terms[e.parent.name]) {
      //otherwise add it
      terms[e.parent.name] = {
        name: e.parent.name,
        visible: true,
        selected: false,
      };
    }
    // if first child then add children array
    if (!terms[e.parent.name].children) {
      terms[e.parent.name].children = [];
    }
    // add the child
    terms[e.parent.name].children.push(terms[e.name]);
  }
});

let rootTerms = computed(() => {
  if (terms) {
    const result = Object.values(terms).filter(
      (t) => !t.parent && t.visible
    );
    return result;
  } else {
    return [];
  }
});

let key = ref(1)
function toggleExpand(term) {
  terms[term.name].expanded = !terms[term.name].expanded;
  key++;
}

</script>

<template>
  <hr class="mx-5 border-black opacity-10" />

  <div class="flex gap-1 p-5 items-center">
    <div class="inline-flex gap-1 group" @click="toggleCollapseTitle()">
      <h3 class="
          text-white
          font-sans
          text-body-base
          font-bold
          mr-[5px]
          group-hover:underline group-hover:cursor-pointer
        ">
        {{ title }}
      </h3>
      <span :class="{ 'rotate-180': collapsedTitle }" class="
          rounded-full
          group-hover:bg-blue-800 group-hover:cursor-pointer
          w-8
          h-8
          text-white
          flex
          items-center
          justify-center
        ">
        <BaseIcon name="caret-up" :width="26" />
      </span>
    </div>
    <div class="grow text-right">
      <span class="
          text-body-sm text-yellow-500
          hover:underline hover:cursor-pointer
        ">Remove 2 selected
      </span>
    </div>
  </div>

  <ul class="ml-5 mb-5 text-white" :class="{ hidden: collapsedTitle }">
    <li v-for="item in rootTerms" :key="item.name" class="mb-2.5">
      <div class="flex items-start">
        <span @click="toggleExpand(item)" :class="{ 'rotate-180': !terms[item.name].expanded }" class="
            text-white
            rounded-full
            hover:bg-blue-800 hover:cursor-pointer
            h-6
            w-6
            flex
            items-center
            justify-center
          ">
          <BaseIcon name="caret-up" :width="20" />
        </span>
        <div class="flex items-center">
          <input type="checkbox" :id="item.name" :name="item.name" class="
              w-5
              h-5
              rounded-3px
              ml-[6px]
              mr-2.5
              mt-0.5
              text-yellow-500
              border-0
            " />
        </div>
        <label :for="item.name" class="hover:cursor-pointer text-body-sm group">
          <span class="group-hover:underline">{{ item.name }}</span>
          <div class="whitespace-nowrap inline-flex items-center">
            <span v-if="item?.children?.length" class="
                text-blue-200
                inline-block
                mr-2
                group-hover:underline
                decoration-blue-200
                fill-black
              " hoverColor="white">&nbsp;- {{ item.children.length }}
            </span>
            <div class="inline-block">
              <CustomTooltip v-if="item.description" label="Read more" hoverColor="white" :content="item.description" />
            </div>
          </div>
        </label>
      </div>

      <ul class="ml-[39px]" :class="{ hidden: !terms[item.name].expanded }" v-if="item.children">
        <SearchFilterGroupChild :data="item.children" />
      </ul>
    </li>
  </ul>
</template>
