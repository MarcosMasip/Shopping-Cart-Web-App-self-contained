import React, { createContext, useContext, ReactNode } from 'react';
import { useCart } from './cart';

// Shape mirrors return of useCart
export interface CartContextValue {
  lines: ReturnType<typeof useCart>['lines'];
  summary: ReturnType<typeof useCart>['summary'];
  loading: ReturnType<typeof useCart>['loading'];
  error: ReturnType<typeof useCart>['error'];
  refresh: ReturnType<typeof useCart>['refresh'];
  addToCart: ReturnType<typeof useCart>['addToCart'];
  removeFromCart: ReturnType<typeof useCart>['removeFromCart'];
}

const CartContext = createContext<CartContextValue | undefined>(undefined);

export function CartProvider({ children }: { children: ReactNode }) {
  const cart = useCart();
  return <CartContext.Provider value={cart}>{children}</CartContext.Provider>;
}

export function useCartContext(): CartContextValue {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error('useCartContext must be used within a CartProvider');
  return ctx;
}

export function useAddToCart(){
  return useCartContext().addToCart;
}
