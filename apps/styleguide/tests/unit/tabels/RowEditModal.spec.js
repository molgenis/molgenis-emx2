import {shallowMount} from '@vue/test-utils';
import RowEditModal from '../../../src/tables/RowEditModal.vue';
import GraphqlRequest from 'graphql-request';

jest.mock('graphql-request');

describe('RowEditModal', () => {
  let wrapper;

  beforeEach(() => {
    const requestResult = {_session: undefined, _schema: undefined};
    GraphqlRequest.mockResolvedValue(requestResult);
    wrapper = shallowMount(RowEditModal, {});
    expect(GraphqlRequest).toBeCalled();
  });

  describe('getRefBackType', () => {
    it("Should return undefined if column type is not 'REFBACK'", () => {
      const column = {
        columnType: 'NOT_REFBACK'
      };
      const result = wrapper.vm.getRefBackType(column);
      expect(result).toBeUndefined();
    });
  });
});
