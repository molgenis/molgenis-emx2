export default {
    startEndYear: function (startYear?: string, endYear?: string) {
        if (startYear && endYear) {
            return startYear + " - " + endYear;
        } else if (startYear) {
            return startYear + " - ongoing";
        } else if (endYear) {
            return "not available - " + endYear;
        } else {
            return "not available";
        }
    }
}
