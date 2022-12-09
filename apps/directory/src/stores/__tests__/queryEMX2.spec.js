import QueryEMX2 from "../queryEMX2";

describe('QueryEMX2 Interface', () => {
    it('can create a simple query on the biobanks table', () => {

        const query = new QueryEMX2('graphql').table('Biobanks').select('id').getQuery()

        expect(query).toStrictEqual({})
    })
})
