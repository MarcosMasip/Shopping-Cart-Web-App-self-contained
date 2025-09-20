import React from 'react';
import { useProducts } from '../state/products';
import { useCart } from '../state/cart';

export default function ProductsPage(){
  const { products, loading, error } = useProducts();
  const { addToCart } = useCart();
  if (loading) return <p>Loading products...</p>;
  if (error) return <p style={{color:'red'}}>Error: {error}</p>;
  return (
    <div className="products">
      {products.map(p => (
        <div key={p.code} className="product">
          <h3>{p.description}</h3>
          <p>Price: ${p.price?.toFixed(2)}</p>
          <p>Stock: {p.qty}</p>
          <button onClick={() => addToCart(p.code, 1)}>Add</button>
        </div>
      ))}
    </div>
  );
}
