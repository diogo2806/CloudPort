import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const source = readFileSync(new URL('./OperatorMode.jsx', import.meta.url), 'utf8');
const model = readFileSync(new URL('./operatorMode.js', import.meta.url), 'utf8');
const style = readFileSync(new URL('./operator-mode.css', import.meta.url), 'utf8');
const components = readFileSync(new URL('./components.jsx', import.meta.url), 'utf8');

test('disponibiliza o modo operador em cada cabeçalho padrão sem remover o manual', () => {
  assert.match(components, /<OperatorModeLauncher \/><ContextHelp \/>/);
  assert.match(source, /aria-label="Abrir modo operador para celular, tablet ou PDA"/);
  assert.match(source, /aria-haspopup="dialog"/);
  assert.match(source, /aria-expanded=\{open\}/);
});

test('workspace e manual possuem semântica modal e nomes acessíveis', () => {
  assert.match(source, /role="dialog" aria-modal="true" aria-labelledby="operator-mode-title"/);
  assert.match(source, /role="dialog" aria-modal="true" aria-labelledby="operator-manual-title"/);
  assert.match(source, /aria-label="Voltar ao modo completo"/);
  assert.match(source, /aria-label="Fila de tarefas"/);
  assert.match(source, /aria-current=\{index === selectedIndex \? 'step' : undefined\}/);
});

test('mantém conectividade, sincronização e pendências sempre descritas', () => {
  assert.match(source, /Online/);
  assert.match(source, /Offline/);
  assert.match(source, /Última sincronização/);
  assert.match(source, /Operações pendentes/);
  assert.match(source, /Sincronizar fila/);
  assert.match(source, /AGUARDANDO_RECONEXAO/);
  assert.match(source, /CONFLITO/);
});

test('carrega somente fontes autorizadas e isola indisponibilidades', () => {
  assert.match(source, /permittedOperatorSources\(session\)/);
  assert.match(source, /sources\.map\(\(source\) => source\.key\)/);
  assert.match(source, /Promise\.allSettled/);
  assert.match(source, /fonte\(s\) indisponível\(is\)/);
  assert.match(model, /filter\(\(source\) => hasAnyRole\(session, source\.roles\)\)/);
});

test('scanner físico, câmera e entrada manual compartilham a validação', () => {
  assert.match(source, /Scanner físico ou entrada manual/);
  assert.match(source, /validateOperatorScan\(value, expectedTypes\(currentTask\)\)/);
  assert.match(source, /new globalThis\.BarcodeDetector\(\)/);
  assert.match(source, /getUserMedia/);
  assert.match(source, /Câmera de código não disponível neste navegador/);
  assert.match(source, /onRead\(codes\[0\]\.rawValue\)/);
});

test('stream da câmera é encerrado no cleanup do componente', () => {
  assert.match(source, /stream\?\.getTracks\?\.\(\)\.forEach\(\(track\) => track\.stop\(\)\)/);
  assert.match(source, /cancelAnimationFrame\(frame\)/);
  assert.doesNotMatch(source, /\[cameraActive, manualOpen, onClose, stopCamera\]/);
});

test('ações críticas exigem leitura válida e resumo explícito', () => {
  assert.match(source, /Operação crítica/);
  assert.match(source, /Objeto lido/);
  assert.match(source, /Origem/);
  assert.match(source, /Destino/);
  assert.match(source, /Equipamento/);
  assert.match(source, /disabled=\{!scan\?\.valid \|\| busy\}/);
});

test('fila aplica chave idempotente e não conclui operação offline localmente', () => {
  assert.match(model, /operatorCommandKey/);
  assert.match(model, /idempotencyKey/);
  assert.match(model, /Nenhuma duplicidade/);
  assert.match(source, /AGUARDANDO_RECONEXAO/);
  assert.match(source, /será enviada somente após a reconexão e nova validação do backend/);
});

test('layout garante toque, 320 px, retrato, paisagem e alto contraste', () => {
  assert.match(style, /min-height: 48px/);
  assert.match(style, /min-width: 320px/);
  assert.match(style, /@media \(max-width: 720px\)/);
  assert.match(style, /@media \(orientation: landscape\) and \(max-height: 620px\)/);
  assert.match(style, /\.operator-mode-layer\.high-contrast/);
  assert.match(style, /@media \(prefers-reduced-motion: reduce\)/);
});

test('manual contém todas as seções exigidas e link do processo completo', () => {
  for (const heading of ['Finalidade', 'Fluxo operacional', 'Campos', 'Permissões', 'Estados', 'Motivos de bloqueio', 'Exemplo', 'Atalhos', 'Processo completo']) {
    assert.match(source, new RegExp(`<h3>${heading}</h3>`));
  }
  assert.match(source, /docs\/manuais\/modo-operador-pda\.md/);
});
