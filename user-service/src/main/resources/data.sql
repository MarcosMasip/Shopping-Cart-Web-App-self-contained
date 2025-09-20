INSERT INTO users (id, username, password_hash, role, created_at) VALUES
  (1,'system', '2a97516c354b68848cdbd8f54a226a0a55b21ed138e207ad6c5cbb9c00aa5aea', 'ROLE_SYSTEM', CURRENT_TIMESTAMP()),
  (2,'admin',  '2a97516c354b68848cdbd8f54a226a0a55b21ed138e207ad6c5cbb9c00aa5aea', 'ROLE_ADMIN',  CURRENT_TIMESTAMP()),
  (3,'demo',   '2a97516c354b68848cdbd8f54a226a0a55b21ed138e207ad6c5cbb9c00aa5aea', 'ROLE_USER',   CURRENT_TIMESTAMP());
