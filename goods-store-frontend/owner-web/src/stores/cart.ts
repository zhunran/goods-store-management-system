import { defineStore } from "pinia";
import { cartList } from "@/api/cart";

/** 购物车数量徽标：登录后拉取一次，加购/删除后手动刷新 */
export const useCartStore = defineStore("cart", {
  state: () => ({
    count: 0,
  }),
  actions: {
    async refresh() {
      try {
        const list = await cartList();
        this.count = list.reduce((sum, item) => sum + (item.qty || 0), 0);
      } catch {
        this.count = 0;
      }
    },
    clear() {
      this.count = 0;
    },
  },
});
