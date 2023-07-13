import { createRouter, createWebHistory } from "vue-router";
import HomePage from "../views/home-view.vue";
import VizBarChart from "../views/viz-bar-chart.vue";
import ColumnChart from "../views/viz-column-chart.vue";
import PieChart from "../views/viz-pie-chart.vue";
import ChartLegend from "../views/viz-chart-legend.vue";
import DataHighlights from "../views/viz-data-highlights.vue";
import GeoMercator from "../views/viz-geo-mercator.vue";
import Datatable from "../views/viz-datatable.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      name: "home",
      path: "/",
      component: HomePage,
    },
    {
      name: "bar-chart",
      path: "/bar-chart",
      component: VizBarChart,
      meta: {
        title: "Bar Chart",
      },
    },
    {
      name: "chart-legend",
      path: "/chart-legend",
      component: ChartLegend,
      meta: {
        title: "Chart Legends",
      },
    },
    {
      name: "column-chart",
      path: "/column-chart",
      component: ColumnChart,
      meta: {
        title: "Column Chart",
      },
    },
    {
      name: "data-highlights",
      path: "/data-highlights",
      component: DataHighlights,
      meta: {
        title: "Data Highlights",
      },
    },
    {
      name: "datatable",
      path: "/datatable",
      component: Datatable,
      meta: {
        title: "Datatable",
      },
    },
    {
      name: "geo-mercator",
      path: "/geo-mercator",
      component: GeoMercator,
      meta: {
        title: "Geomercator",
      },
    },
    {
      name: "pie-chart",
      path: "/pie-chart",
      component: PieChart,
      meta: {
        title: "Pie Chart",
      },
    },
  ],
  scrollBehavior() {
    return {
      top: 0,
    };
  },
});

router.afterEach((to) => {
  document.title = to.meta.title
    ? `${to.meta.title} | RD-Components`
    : "RD-Components";
});

export default router;
