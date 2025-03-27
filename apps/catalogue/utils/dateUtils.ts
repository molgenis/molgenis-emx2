export default {
  startEndYear: function (startYear?: number, endYear?: number) {
    if (startYear && endYear) {
      return startYear + " until " + endYear;
    } else if (startYear) {
      return startYear + " (ongoing)";
    } else if (endYear) {
      return "(start not available) until " + endYear;
    } else {
      return "not available";
    }
  },
};
