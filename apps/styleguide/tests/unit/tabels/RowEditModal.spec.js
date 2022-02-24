import {shallowMount} from '@vue/test-utils';
import RowEditModal from '../../../src/tables/RowEditModal.vue';
import GraphqlRequest from 'graphql-request';
import Vue from 'vue';

jest.mock('graphql-request');

describe('RowEditModal', () => {
  describe('getRefBackType', () => {
    it("Should return undefined if column type is not 'REFBACK'", () => {
      const requestResult = {_session: undefined, _schema: undefined};
      GraphqlRequest.mockResolvedValue(requestResult);
      const wrapper = shallowMount(RowEditModal, {});
      expect(GraphqlRequest).toBeCalled();

      const column = {
        columnType: 'NOT_REFBACK'
      };
      const result = wrapper.vm.getRefBackType(column);
      expect(result).toBeUndefined();
    });

    it('Should return the type (string) of the referenced column', async (done) => {
      const tableName = 'TableName';
      const schema = {
        tables: [
          {
            name: tableName,
            columns: [{name: 'ColumnName', columnType: 'Succes'}]
          }
        ]
      };
      const requestResult = {_session: undefined, _schema: schema};
      GraphqlRequest.mockResolvedValue(requestResult);
      const wrapper = shallowMount(RowEditModal, {});
      expect(GraphqlRequest).toBeCalled();

      const column = {
        columnType: 'REFBACK',
        refBack: 'ColumnName',
        refTable: tableName
      };

      await Vue.nextTick();
      const result = wrapper.vm.getRefBackType(column);
      expect(result).toBe('Succes');
      done();
    });
  });
});
