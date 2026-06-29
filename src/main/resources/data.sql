-- Demo user (id=1 — matches USER_ID in the frontend)
INSERT INTO users (name, email, password, active)
VALUES ('Demo User', 'demo@harness.io', '1harness', true);

-- Sample products
INSERT INTO products (name, description, price, stock, category, active) VALUES
  ('Wireless Headphones',    'Noise-cancelling over-ear headphones with 30h battery',  79.99,  45, 'Electronics', true),
  ('Mechanical Keyboard',    'TKL mechanical keyboard with RGB backlight',             119.99,  30, 'Electronics', true),
  ('USB-C Hub',              '7-in-1 USB-C hub with HDMI, USB 3.0 and SD card slots',  34.99,  60, 'Electronics', true),
  ('Laptop Stand',           'Adjustable aluminium laptop stand, 10–15 inch',          29.99,  80, 'Accessories', true),
  ('Webcam 1080p',           'Full HD webcam with built-in microphone',                49.99,  25, 'Electronics', true),
  ('Desk Lamp LED',          'Touch-dimming LED desk lamp, USB charging port',         24.99,  55, 'Home Office', true),
  ('Mouse Pad XL',           'Extended gaming mouse pad 900 x 400 mm',                 19.99, 100, 'Accessories', true),
  ('Ergonomic Chair',        'Lumbar support mesh chair for long work sessions',       249.99,  10, 'Furniture',   true),
  ('Standing Desk Mat',      'Anti-fatigue mat for standing desks',                    39.99,  40, 'Furniture',   true),
  ('Cable Management Kit',   'Velcro straps, clips and tray for a tidy desk',          14.99,  75, 'Accessories', true),
  ('Portable SSD 1TB',       'USB 3.2 portable solid-state drive, 1000 MB/s read',    89.99,  35, 'Electronics', true),
  ('Smart Plug',             'Wi-Fi smart plug with energy monitoring',                15.99,  90, 'Smart Home',  true);
