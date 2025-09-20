import { useEffect, useState, useCallback } from 'react';

export interface CartLineDTO {
  id: string;
  itemCode: number;
  description: string;
  qty: number;
  unitPrice: number;
  lineTotal: number;
}
export interface CartSummaryDTO { items: CartLineDTO[]; total: number; }

function basicAuthHeader(){
  // Demo only; embed a static user. Real implementation: user login flow.
  return 'Basic ' + btoa('demo:demo');
}

export function useCart(){
  const [lines, setLines] = useState<CartLineDTO[]>([]);
  const [summary, setSummary] = useState<CartSummaryDTO | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(() => {
    setLoading(true);
    fetch('/api/v1/cart-items', { headers: { authorization: basicAuthHeader() }})
      .then(r => { if(!r.ok) throw new Error('Load cart failed'); return r.json(); })
      .then(data => setLines(data))
      .then(() => fetch('/api/v1/cart-items/summary', { headers: { authorization: basicAuthHeader() }}))
      .then(r => { if(!r.ok) throw new Error('Summary failed'); return r.json(); })
      .then(sum => setSummary(sum))
      .catch(e => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { refresh(); }, [refresh]);

  function addToCart(itemCode: number, qty: number){
    fetch('/api/v1/cart-items', { method: 'POST', headers: { 'Content-Type':'application/json', authorization: basicAuthHeader() }, body: JSON.stringify({ itemCode, qty })})
      .then(r => { if(!r.ok) throw new Error('Add failed'); refresh(); })
      .catch(e => setError(e.message));
  }
  function removeFromCart(itemCode: number){
    fetch(`/api/v1/cart-items/${itemCode}`, { method: 'DELETE', headers: { authorization: basicAuthHeader() }})
      .then(r => { if(!r.ok) throw new Error('Remove failed'); refresh(); })
      .catch(e => setError(e.message));
  }
  return { lines, summary, loading, error, refresh, addToCart, removeFromCart };
}

// Simple facade for components
export function useAddToCart(){
  return useCart().addToCart;
}
