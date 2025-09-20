import React, { useEffect } from 'react';
import { useCart } from '../state/cart';

export default function CartPage(){
  const { lines, summary, loading, error, refresh, removeFromCart } = useCart();
  useEffect(() => { refresh(); }, [refresh]);
  if (loading) return <p>Loading cart...</p>;
  if (error) return <p style={{color:'red'}}>Error: {error}</p>;
  return (
    <div className="cart">
      <h2>Your Cart</h2>
      {lines.length === 0 && <p>No items yet.</p>}
      {lines.map(line => (
        <div key={line.id.toString()} className="cart-line">
          <strong>{line.description}</strong> x {line.qty} @ ${line.unitPrice.toFixed(2)} = ${line.lineTotal.toFixed(2)}
          <button onClick={() => removeFromCart(line.itemCode)}>Remove</button>
        </div>
      ))}
      {lines.length > 0 && <h3>Total: ${summary?.total.toFixed(2)}</h3>}
    </div>
  );
}
