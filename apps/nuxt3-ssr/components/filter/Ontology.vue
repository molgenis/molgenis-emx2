<script setup>
const props = defineProps({
  tableName: {
    type: String,
  },
  isMultiSelect: {
    type: Boolean,
    default: true,
  },
  modelValue: {
    type: Array,
    default: [],
  },
  options: {
    type: Array,
  },
  mobileDisplay: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(["update:modelValue"]);

let data = [];
if (!props.options) {
  const query = `
    query 
    ${props.tableName}( $filter:${props.tableName}Filter, $orderby:${props.tableName}orderby )
    {   
      ${props.tableName}( filter:$filter, limit:100000,  offset:0, orderby:$orderby )  
        {          
          order
          name 
          code 
          parent{ name }
          ontologyTermURI 
          definition 
          children{ name }
        }       
      ${props.tableName}_agg( filter:$filter ) { count }
      }
  `;
  let resp = await fetchOntology(query);

  data = resp?.data[props.tableName];
  let count = resp?.data[props.tableName + "_agg"].count;
} else {
  data = props.options;
}

// convert to tree of terms
//list all terms, incl subtrees
let terms = reactive({});
data.forEach(e => {
  // did we see it maybe as parent before?
  if (terms[e.name]) {
    //then copy properties, currently only definition
    terms[e.name].definition = e.definition;
  } else {
    //else simply add the record
    terms[e.name] = {
      name: e.name,
      order: e.order,
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
    const result = Object.values(terms).filter(t => !t.parent && t.visible);
    return result;
  } else {
    return [];
  }
});

function toggleExpand(term) {
  terms[term.name].expanded = !terms[term.name].expanded;
}

function select(item) {
  if (!props.isMultiSelect) {
    //deselect other items
    Object.keys(terms).forEach(key => (terms[key].selected = false));
  }
  let term = terms[item];
  term.selected = "complete";
  if (props.isMultiSelect) {
    //if list also select also its children
    getAllChildren(term).forEach(
      childTerm => (childTerm.selected = "complete")
    );
    //select parent(s) if all siblings are selected
    getParents(term).forEach(parent => {
      if (parent.children.every(childTerm => childTerm.selected)) {
        parent.selected = "complete";
      } else {
        parent.selected = "partial";
      }
    });
  }
  emitValue();
}

function deselect(item) {
  if (props.isMultiSelect) {
    let term = terms[item];
    term.selected = false;
    //also deselect all its children
    getAllChildren(terms[item]).forEach(
      childTerm => (childTerm.selected = false)
    );
    //also its deselect its parents, might be partial
    getParents(term).forEach(parent => {
      if (parent.children.some(child => child.selected)) {
        parent.selected = "partial";
      } else {
        parent.selected = false;
      }
    });
  } else {
    //non-list, deselect all
    Object.keys(terms).forEach(name => (terms[name].selected = false));
  }
  emitValue();
  // $refs.search.focus();
}

function getParents(term) {
  let result = [];
  let parent = term.parent;
  while (parent) {
    result.push(terms[parent.name]);
    if (
      terms[parent.name].parent &&
      //check for parent that are indirect parent of themselves
      !result.includes(terms[parent.name].parent.name)
    ) {
      parent = terms[parent.name].parent;
    } else {
      parent = null;
    }
  }
  return result;
}

function getAllChildren(term) {
  let result = [];
  if (term.children) {
    result = term.children;
    term.children.forEach(
      childTerm => (result = result.concat(getAllChildren(childTerm)))
    );
  }
  return result;
}

function emitValue() {
  let selectedTerms = Object.values(terms)
    .filter(term => term.selected === "complete" && !term.children)
    .map(term => {
      return { name: term.name };
    });
  if (props.isMultiSelect) {
    emit("update:modelValue", selectedTerms);
  } else {
    emit("update:modelValue", selectedTerms[0]);
  }
}

function toggleSelect(term) {
  //if selecting then also expand
  //if deselection we keep it open
  if (term.selected == "complete") {
    deselect(term.name);
  } else {
    select(term.name);
  }
}

watch(() => props.modelValue, updateSelection, { deep: true });

function updateSelection(newConditions) {
  if (!newConditions.length) {
    Object.values(terms).forEach(term => (term.selected = false));
  }
}
</script>

<template>
  <ul>
    <li v-for="item in rootTerms" :key="item.name" class="mb-2.5">
      <div class="flex items-start">
        <span
          v-if="item.children"
          @click="toggleExpand(item)"
          class="flex items-center justify-center w-6 h-6 rounded-full hover:bg-search-filter-group-toggle hover:cursor-pointer"
          :class="{
            'rotate-180': !terms[item.name].expanded,
            'text-search-filter-group-toggle-mobile': mobileDisplay,
            'text-search-filter-group-toggle': !mobileDisplay,
          }">
          <BaseIcon name="caret-up" :width="20" />
        </span>
        <span
          v-else
          class="flex items-center justify-center w-6 h-6 rounded-full hover:bg-search-filter-group-toggle hover:cursor-pointer"
          :class="`text-search-filter-group-toggle${
            mobileDisplay ? '-mobile' : ''
          }`">
        </span>
        <div class="flex items-center">
          <input
            type="checkbox"
            :id="item.name"
            :name="item.name"
            :checked="
              item.selected === 'complete' || item.selected === 'partial'
            "
            @click.stop="toggleSelect(item)"
            :class="{
              'text-yellow-500': item.selected === 'complete',
              'text-search-filter-group-checkbox': item.selected !== 'complete',
            }"
            class="w-5 h-5 rounded-3px ml-[6px] mr-2.5 mt-0.5 border border-checkbox" />
        </div>
        <label :for="item.name" class="hover:cursor-pointer text-body-sm group">
          <span class="group-hover:underline">{{ item.name }}</span>
          <div class="inline-flex items-center whitespace-nowrap">
            <!--
            <span
              v-if="item?.children?.length"
              class="inline-block mr-2 text-blue-200 group-hover:underline decoration-blue-200 fill-black"
              hoverColor="white"
              >&nbsp;- {{ item.children.length }}
            </span>
            -->
            <div class="inline-block">
              <CustomTooltip
                v-if="item.description"
                label="Read more"
                hoverColor="white"
                :content="item.description" />
            </div>
          </div>
        </label>
      </div>

      <ul
        class="ml-10 mr-4"
        :class="{ hidden: !terms[item.name].expanded }"
        v-if="item.children">
        <FilterOntologyChild
          :items="item.children"
          @select="select"
          @deselect="deselect" />
      </ul>
    </li>
  </ul>
</template>
