import { useEffect, useState } from 'react';

export interface ProductDTO {
  code: number;
  description: string;
  qty: number;
  price?: number;
  createdAt?: string;
}

export function useProducts(){
  const [products, setProducts] = useState<ProductDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    let active = true;
    fetch('/api/v1/items')
      .then(r => {
        if(!r.ok) throw new Error('Failed to load products');
        return r.json();
      })
      .then(data => { if(active) { setProducts(data); setLoading(false); } })
      .catch(e => { if(active){ setError(e.message); setLoading(false);} });
    return () => { active = false; };
  }, []);
  return { products, loading, error };
}
