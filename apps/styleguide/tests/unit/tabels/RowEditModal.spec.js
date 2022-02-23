import {shallowMount} from '@vue/test-utils';
import RowEditModal from '../../../src/tables/RowEditModal.vue';

describe('RowEditModal', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallowMount(RowEditModal, {});
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
