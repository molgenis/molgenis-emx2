import {shallowMount} from '@vue/test-utils';
import RowEditModal from '../../../src/tables/RowEditModal.vue';
import GraphqlRequest from 'graphql-request';
import Vue from 'vue';
import Expressions from '@molgenis/expressions';

jest.mock('@molgenis/expressions');
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

  describe('showColumn', () => {
    it('should return truthy if the column should be shown', () => {
      const wrapper = shallowMount(RowEditModal, {});
      const column = {
        refLink: undefined,
        columnType: 'NOT_REFBACK',
        id: 'ColumnName'
      };
      const result = wrapper.vm.showColumn(column);
      expect(result).toBe(true);
    });

    it('should return truthy if the column should be shown', () => {
      const wrapper = shallowMount(RowEditModal, {
        propsData: {
          defaultValue: {
            refLink: {}
          }
        }
      });
      const column = {
        refLink: 'refLink',
        columnType: 'NOT_REFBACK',
        id: 'ColumnName'
      };
      const result = wrapper.vm.showColumn(column);
      expect(result).toBeTruthy();
    });

    it('should return truthy if the column should be shown', () => {
      const wrapper = shallowMount(RowEditModal, {
        propsData: {visibleColumns: ['ColumnName']}
      });
      const column = {
        refLink: undefined,
        columnType: 'NOT_REFBACK',
        id: 'ColumnName'
      };
      const result = wrapper.vm.showColumn(column);
      expect(result).toBeTruthy();
    });

    it('should return falsy if the column is named mg_tableclass', () => {
      const wrapper = shallowMount(RowEditModal, {});
      const column = {
        refLink: undefined,
        columnType: 'NOT_REFBACK',
        id: 'ColumnName',
        name: 'mg_tableclass'
      };
      const result = wrapper.vm.showColumn(column);
      expect(result).toBeFalsy();
    });

    it('should return falsy if the column has a reference value', () => {
      const wrapper = shallowMount(RowEditModal, {});
      const column = {
        refLink: 'refLink',
        columnType: 'NOT_REFBACK',
        id: 'ColumnName'
      };
      const result = wrapper.vm.showColumn(column);
      expect(result).toBeFalsy();
    });

    it('should return falsy if the column has a reference value', () => {
      const wrapper = shallowMount(RowEditModal, {
        propsData: {visibleColumns: []}
      });
      const column = {
        refLink: 'refLink',
        columnType: 'NOT_REFBACK',
        id: 'ColumnName'
      };
      const result = wrapper.vm.showColumn(column);
      expect(result).toBeFalsy();
    });
  });

  describe('visible', () => {
    const columnId = 'ColumnId';

    it('should return true is there is no expression', () => {
      const wrapper = shallowMount(RowEditModal, {});
      const expression = undefined;
      const result = wrapper.vm.visible(expression, columnId);
      expect(result).toBeTruthy();
    });

    it('should return true is there is a expression and is evaluates successfully', () => {
      Expressions.evaluate.mockReturnValue(true);
      const wrapper = shallowMount(RowEditModal, {});
      const expression = 'some expression';
      const result = wrapper.vm.visible(expression, columnId);
      expect(result).toBeTruthy();
    });

    it('should return false is there is a expression and is evaluates successfully', () => {
      Expressions.evaluate.mockReturnValue(false);
      const wrapper = shallowMount(RowEditModal, {});
      const expression = 'some expression';
      const result = wrapper.vm.visible(expression, columnId);
      expect(result).toBeFalsy();
    });
  });

  describe('isRefLinkWithoutOverlap', () => {
    it('should return false if the column has no refLink', () => {
      const wrapper = shallowMount(RowEditModal, {});
      const column = {
        refLink: undefined
      };
      const result = wrapper.vm.isRefLinkWithoutOverlap(column);
      expect(result).toBeFalsy();
    });

    it('should return true if the column value and the refLink value share no overlap', async (done) => {
      const tableName = 'TableName';
      const schema = {
        tables: [
          {
            name: tableName,
            columns: [{name: 'RefName', id: 'RefId'}]
          }
        ]
      };
      const requestResult = {_session: undefined, _schema: schema};
      GraphqlRequest.mockResolvedValue(requestResult);
      const wrapper = shallowMount(RowEditModal, {
        propsData: {
          table: tableName,
          defaultValue: {ColumnId: 'ColumnValue', RefId: 'RefValue'}
        }
      });
      const column = {
        refLink: 'RefName',
        id: 'ColumnId'
      };
      await Vue.nextTick();
      const result = wrapper.vm.isRefLinkWithoutOverlap(column);
      expect(result).toBeTruthy();
      done();
    });

    it('should return false if the column value and the refLink value share overlap when they are string', async (done) => {
      const tableName = 'TableName';
      const schema = {
        tables: [
          {
            name: tableName,
            columns: [{name: 'RefName', id: 'RefId'}]
          }
        ]
      };
      const requestResult = {_session: undefined, _schema: schema};
      GraphqlRequest.mockResolvedValue(requestResult);
      const wrapper = shallowMount(RowEditModal, {
        propsData: {
          table: tableName,
          defaultValue: {ColumnId: 'ColumnValue', RefId: 'Value'}
        }
      });
      const column = {
        refLink: 'RefName',
        id: 'ColumnId'
      };
      await Vue.nextTick();
      const result = wrapper.vm.isRefLinkWithoutOverlap(column);
      expect(result).toBeFalsy();
      done();
    });

    it('should return false if the column value and the refLink value share overlap when they are not strings', async (done) => {
      const tableName = 'TableName';
      const schema = {
        tables: [
          {
            name: tableName,
            columns: [{name: 'RefName', id: 'RefId'}]
          }
        ]
      };
      const requestResult = {_session: undefined, _schema: schema};
      GraphqlRequest.mockResolvedValue(requestResult);
      const wrapper = shallowMount(RowEditModal, {
        propsData: {
          table: tableName,
          defaultValue: {ColumnId: 1234, RefId: 123}
        }
      });
      const column = {
        refLink: 'RefName',
        id: 'ColumnId'
      };
      await Vue.nextTick();
      const result = wrapper.vm.isRefLinkWithoutOverlap(column);
      expect(result).toBeFalsy();
      done();
    });
  });
});
