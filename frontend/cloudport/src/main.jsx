import React from 'react';
import { createRoot } from 'react-dom/client';
import App from './App.jsx';
import { loadRuntimeConfig } from './api.js';
import './styles.css';
import './gateVisual.css';
import './steel-coil-planner.css';
import './vessel-lineup.css';
import './rail-lineup.css';
import './navio-operational.css';

function renderConfigurationError(error) {
  createRoot(document.getElementById('root')).render(
    <main className="configuration-error">
      <section>
        <span className="eyebrow">Falha de configuração</span>
        <h1>Não foi possível iniciar o CloudPort.</h1>
        <p>{error?.message ?? 'Verifique o arquivo assets/configuracao.json e tente novamente.'}</p>
      </section>
    </main>
  );
}

loadRuntimeConfig()
  .then(() => createRoot(document.getElementById('root')).render(<React.StrictMode><App /></React.StrictMode>))
  .catch(renderConfigurationError);
