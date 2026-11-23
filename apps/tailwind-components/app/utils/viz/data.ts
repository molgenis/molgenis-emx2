import type { IChartData } from "../../../types/cms";

export const recruitmentDataExample: IChartData[] = [
  { id: "value-jan", xValue: "Jan", yValue: "18" },
  { id: "value-feb", xValue: "Feb", yValue: "33" },
  { id: "value-mar", xValue: "Mar", yValue: "56" },
  { id: "value-apr", xValue: "Apr", yValue: "75" },
  { id: "value-may", xValue: "May", yValue: "22" },
  { id: "value-jun", xValue: "Jun", yValue: "10" },
];

export const recruitmentTargetExample: IChartData[] = [
  { id: "value-jan", xValue: "Jan", yValue: "-32", fillColor: "#e14f62" },
  { id: "value-feb", xValue: "Feb", yValue: "17", yLabel: "+17" },
  { id: "value-mar", xValue: "Mar", yValue: "6", yLabel: "+6" },
  { id: "value-apr", xValue: "Apr", yValue: "-25", fillColor: "#e14f62" },
  { id: "value-may", xValue: "May", yValue: "-28", fillColor: "#e14f62" },
  { id: "value-jun", xValue: "Jun", yValue: "-40", fillColor: "#e14f62" },
];

export const recruitmentByGender: IChartData[] = [
  {
    id: "center-a-female",
    xValue: "Female",
    yValue: "15",
    primaryGroupValue: "Center A",
  },
  {
    id: "center-a-male",
    xValue: "Male",
    yValue: "13",
    primaryGroupValue: "Center A",
  },
  {
    id: "center-a-other",
    xValue: "Other",
    yValue: "5",
    primaryGroupValue: "Center A",
  },
  {
    id: "center-b-female",
    xValue: "Female",
    yValue: "13",
    primaryGroupValue: "Center B",
  },
  {
    id: "center-b-male",
    xValue: "Male",
    yValue: "2",
    primaryGroupValue: "Center B",
  },
  {
    id: "center-b-other",
    xValue: "Other",
    yValue: "1",
    primaryGroupValue: "Center B",
  },
  {
    id: "center-c-female",
    xValue: "Female",
    yValue: "6",
    primaryGroupValue: "Center C",
  },
  {
    id: "center-c-male",
    xValue: "Male",
    yValue: "7",
    primaryGroupValue: "Center C",
  },
  {
    id: "center-c-other",
    xValue: "Other",
    yValue: "2",
    primaryGroupValue: "Center C",
  },
  {
    id: "center-d-female",
    xValue: "Female",
    yValue: "2",
    primaryGroupValue: "Center D",
  },
  {
    id: "center-d-male",
    xValue: "Male",
    yValue: "2",
    primaryGroupValue: "Center D",
  },
  {
    id: "center-d-other",
    xValue: "Other",
    yValue: "0",
    primaryGroupValue: "Center D",
  },
  {
    id: "center-e-female",
    xValue: "Female",
    yValue: "1",
    primaryGroupValue: "Center E",
  },
  {
    id: "center-e-male",
    xValue: "Male",
    yValue: "0",
    primaryGroupValue: "Center E",
  },
  {
    id: "center-e-other",
    xValue: "Other",
    yValue: "0",
    primaryGroupValue: "Center E",
  },
];
