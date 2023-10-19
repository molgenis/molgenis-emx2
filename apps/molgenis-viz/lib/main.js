// display components
import Accordion from "../src/components/display/Accordion.vue";
import ActionLink from "../src/components/display/ActionLink.vue";
import InfoCard from "../src/components/display/InfoCard.vue";
import LinkCard from "../src/components/display/LinkCard.vue";
import LoadingScreen from "../src/components/display/LoadingScreen.vue";
import MessageBox from "../src/components/display/MessageBox.vue";
import MolgenisLogo from "../src/components/display/MolgenisLogo.vue";
import UnorderedList from "../src/components/display/UnorderedList.vue";

// forms
import ButtonSearch from "../src/components/forms/ButtonSearch.vue";
import InputLabel from "../src/components/forms/InputLabel.vue";
import InputSearch from "../src/components/forms/InputSearch.vue";

// layout components
import Dashboard from "../src/components/layouts/Dashboard.vue";
import DashboardBox from "../src/components/layouts/DashboardBox.vue";
import Page from "../src/components/layouts/Page.vue";
import PageFooter from "../src/components/layouts/PageFooter.vue";
import PageFooterMeta from "../src/components/layouts/PageFooterMeta.vue";
import PageFooterMolgenisCitation from "../src/components/layouts/PageFooterMolgenisCitation.vue";
import PageForm from "../src/components/layouts/PageForm.vue";
import PageFormSection from "../src/components/layouts/PageFormSection.vue";
import PageHeader from "../src/components/layouts/PageHeader.vue";
import PageSection from "../src/components/layouts/PageSection.vue";

// visualisations
import BarChart from "../src/components/viz/BarChart.vue";
import ChartLegend from "../src/components/viz/ChartLegend.vue";
import ColumnChart from "../src/components/viz/ColumnChart.vue";
import DataTable from "../src/components/viz/DataTable.vue";
import DataValueHighlights from "../src/components/viz/DataHighlights.vue";
import GeoMercator from "../src/components/viz/GeoMercator.vue";
import GroupedColumnChart from "../src/components/viz/GroupedColumnChart.vue";
import PieChart from "../src/components/viz/PieChart.vue";
import PieChart2 from "../src/components/viz/PieChart2.vue";
import ProgressMeter from "../src/components/viz/ProgressMeter.vue";
import ScatterPlot from "../src/components/viz/ScatterPlot.vue";

// data
import WorldGeoJson from "../src/data/world.geo.json";

// utils
import {
  asDataObject,
  flattenData,
  renameKey,
  sortData,
  reverseSortData,
} from "../src/utils/utils.js";

export {
  // display components
  Accordion,
  ActionLink,
  ButtonSearch,
  InfoCard,
  InputLabel,
  InputSearch,
  LinkCard,
  LoadingScreen,
  MessageBox,
  MolgenisLogo,

  // layouts
  Dashboard,
  DashboardBox,
  Page,
  PageFooter,
  PageFooterMeta,
  PageFooterMolgenisCitation,
  PageForm,
  PageFormSection,
  PageHeader,
  PageSection,
  UnorderedList,

  // visualisations
  BarChart,
  ChartLegend,
  ColumnChart,
  DataTable,
  DataValueHighlights,
  GeoMercator,
  GroupedColumnChart,
  PieChart,
  PieChart2,
  ProgressMeter,
  ScatterPlot,

  // datasets
  WorldGeoJson,

  // utils
  asDataObject,
  flattenData,
  renameKey,
  sortData,
  reverseSortData,
};
