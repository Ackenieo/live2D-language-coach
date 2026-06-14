import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App.tsx';
import './index.css';

const rootElement = document.getElementById('root');
if (!rootElement) {
  throw new Error('Root element #root not found');
}

Object.assign(document.documentElement.style, {
  minHeight: '100%',
});

Object.assign(document.body.style, {
  margin: '0',
  minHeight: '100%',
  background: '#070a0f',
});

Object.assign(rootElement.style, {
  minHeight: '100%',
});

createRoot(rootElement).render(
  <BrowserRouter>
    <App />
  </BrowserRouter>,
);
