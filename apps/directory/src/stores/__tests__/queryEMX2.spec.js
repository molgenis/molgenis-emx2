import QueryEMX2 from "../queryEMX2";

describe('QueryEMX2 Interface', () => {
    it('can create a simple query on the biobanks table', () => {
        const query = new QueryEMX2('graphql').table('Biobanks').select('id').getQuery()

        expect(query).toStrictEqual(`{
            Biobanks {
               id
              }
            }`
        )
    })

    it('can create a query when selecting multiple columns on the biobanks table', () => {
        const query = new QueryEMX2('graphql').table('Biobanks').select(['id', 'name']).getQuery()

        expect(query).toStrictEqual(`{
            Biobanks {
               id,name
              }
            }`
        )
    })

    it('can create a query with a filter', () => {
        const query = new QueryEMX2('graphql')
        .table('Biobanks')
        .select(['id', 'name'])
        .where('name').like('UMC')
        .getQuery()

        expect(query).toStrictEqual(`{
            Biobanks(filter: { name: { like: "UMC"} }) {
               id,name
              }
            }`
        )
    })

    it('can create a query with and by default when multiple filters are applied', () => {
        const query = new QueryEMX2('graphql')
        .table('Biobanks')
        .select(['id', 'name'])
        .where('name').like('UMC')
        .where('country').equals('Germany')
        .getQuery()

        expect(query).toStrictEqual(`{
            Biobanks(filter: { name: { like: "UMC"}, _and: { country: { equals: "Germany"} }}) {
               id,name
              }
            }`
        )
    })

    it('can create a query with an and clause', () => {
        const query = new QueryEMX2('graphql')
        .table('Biobanks')
        .select(['id', 'name'])
        .where('name').like('UMC')
        .and('country').equals('Germany')
        .getQuery()

        expect(query).toStrictEqual(`{
            Biobanks(filter: { name: { like: "UMC"}, _and: { country: { equals: "Germany"} }}) {
               id,name
              }
            }`
        )
    })

    it('can create a query with an or clause', () => {
        const query = new QueryEMX2('graphql')
        .table('Biobanks')
        .select(['id', 'name'])
        .where('name').like('UMC')
        .or('country').equals('Germany')
        .getQuery()

        expect(query).toStrictEqual(`{
            Biobanks(filter: { name: { like: "UMC"}, _or: { country: { equals: "Germany"} }}) {
               id,name
              }
            }`
        )
    })
})
