module.exports = {
  root: true,
  env: { browser: true, es2021: true, node: true, jest: false },
  parser: '@typescript-eslint/parser',
  parserOptions: { ecmaVersion: 'latest', sourceType: 'module' },
  plugins: ['@typescript-eslint','react-refresh'],
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
  ],
  settings: { react: { version: 'detect' } },
  rules: {
    'no-unused-vars': 'warn',
    '@typescript-eslint/no-explicit-any': 'off'
  }
};
