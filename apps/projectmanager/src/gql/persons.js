import gql from "graphql-tag";
export default gql`
    {
        Persons(orderby:{name:ASC}) {
            name
            fTE
            planning(filter: {endDate : { between: ["${new Date().toISOString()}",null]}}) {
                projectUnit {
                    project {
                        name
                    }
                    unit
                }
                startDate
                endDate
                fTE
            }
        }
    }

`;
