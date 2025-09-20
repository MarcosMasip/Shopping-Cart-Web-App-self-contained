import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import React from 'react';
import ProductsPage from '../pages/ProductsPage';

const mockItems = [
  { code: 1, description: 'Widget', price: 5.5, qty: 10 },
  { code: 2, description: 'Gadget', price: 3.25, qty: 4 }
];

describe('ProductsPage', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => mockItems } as any));
  });

  it('renders product list', async () => {
    render(<ProductsPage />);
    await waitFor(() => expect(screen.getByText('Widget')).toBeInTheDocument());
    expect(screen.getByText('Gadget')).toBeInTheDocument();
  });
});