import service, { del, get, post, put } from "@/utils/request";
import type { CartItemVO } from "./types";

export function cartList() {
  return get<CartItemVO[]>("/cart/list");
}

export function cartAdd(goodId: string | number, qty = 1) {
  return post<void>("/cart/add", { goodId, qty });
}

export function cartUpdateQty(cartId: string | number, qty: number) {
  return put<void>(`/cart/${cartId}`, null, { qty });
}

export function cartRemove(cartId: string | number) {
  return del<void>(`/cart/${cartId}`);
}

export function cartUpdateSelected(
  cartIds: (string | number)[],
  selected: boolean,
) {
  return put<void>("/cart/selected", { cartIds, selected });
}

export function cartRemoveBatch(cartIds: (string | number)[]) {
  return service.delete("/cart/batch", { data: { cartIds } }) as Promise<void>;
}
