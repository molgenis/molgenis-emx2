import { createRouter, createWebHashHistory } from "vue-router";

import schema_home from "../views/schema-home.vue";
import schema_settings from "../views/schema-settings.vue";

import docs_home_page from "../views/view-home.vue";
import docs_demo from "../views/viz-demo.vue";
import docs_bar_chart from "../views/viz-bar-chart.vue";
import docs_chart_legend from "../views/viz-chart-legend.vue";
import docs_column_chart from "../views/viz-column-chart.vue";
import docs_data_highlights from "../views/viz-data-highlights.vue";
import docs_datatables from "../views/viz-datatable.vue";
import docs_progress_charts from "../views/viz-progress-charts.vue";
import docs_geo_mercator from "../views/viz-geo-mercator.vue";
import docs_grouped_column_chart from "../views/viz-grouped-column-chart.vue";
import docs_pie_chart from "../views/viz-pie-chart.vue";
import docs_pie_chart_2 from "../views/viz-pie-chart-2.vue";
import docs_scatter_plot from "../views/viz-scatter-plot.vue";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      name: "home",
      path: "/",
      redirect: {
        name: "schema-dashboard",
      },
      children: [
        {
          name: "schema-dashboard",
          path: "/",
          component: schema_home,
        },
        {
          names: "schema-settings",
          path: "/settings",
          component: schema_settings,
          meta: {
            title: "Visualization Settings",
          },
        },
      ],
    },
    {
      name: "docs",
      path: "/docs",
      redirect: {
        name: "docs-home",
      },
      children: [
        {
          name: "docs-home",
          path: "/docs/",
          component: docs_home_page,
        },
        {
          name: "docs-demo",
          path: "/docs/demo",
          component: docs_demo,
          meta: {
            title: "Demo",
          },
        },
        {
          name: "bar-chart",
          path: "/docs/bar-chart",
          component: docs_bar_chart,
          meta: {
            title: "Bar Chart",
          },
        },
        {
          name: "chart-legend",
          path: "/docs/chart-legend",
          component: docs_chart_legend,
          meta: {
            title: "Chart Legends",
          },
        },
        {
          name: "column-chart",
          path: "/docs/column-chart",
          component: docs_column_chart,
          meta: {
            title: "Column Chart",
          },
        },
        {
          name: "data-highlights",
          path: "/docs/data-highlights",
          component: docs_data_highlights,
          meta: {
            title: "Data Highlights",
          },
        },
        {
          name: "docs_datatables",
          path: "/docs/docs_datatables",
          component: docs_datatables,
          meta: {
            title: "Data Table",
          },
        },
        {
          name: "progress-charts",
          path: "/docs/progress-charts",
          component: docs_progress_charts,
          meta: {
            title: "Progress Charts",
          },
        },
        {
          name: "progress-charts",
          path: "/docs/progress-charts",
          component: docs_progress_charts,
          meta: {
            title: "Progress Charts",
          },
        },
        {
          name: "grouped-column-chart",
          path: "/docs/grouped-column-chart",
          component: docs_grouped_column_chart,
          meta: {
            title: "Grouped Column Chart",
          },
        },
        {
          name: "geo-mercator",
          path: "/docs/geo-mercator",
          component: docs_geo_mercator,
          meta: {
            title: "docs_geo_mercator",
          },
        },
        {
          name: "pie-chart",
          path: "/docs/pie-chart",
          component: docs_pie_chart,
          meta: {
            title: "Pie Chart",
          },
        },
        {
          name: "pie-chart-2",
          path: "/docs/pie-chart-2",
          component: docs_pie_chart_2,
          meta: {
            title: "Pie Chart 2",
          },
        },
        {
          name: "scatter-plot",
          path: "/docs/scatter-plot",
          component: docs_scatter_plot,
          meta: {
            title: "Scatter Plot",
          },
        },
      ],
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
    ? `${to.meta.title} | Molgenis Viz`
    : "Molgenis Viz";
});

export default router;
