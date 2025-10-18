import { test, expect } from '@playwright/test';

test.use({
  storageState: {
    cookies: [],
    origins: [
      {
        origin: 'http://127.0.0.1:4300',
        localStorage: [
          {
            name: 'currentUser',
            value: JSON.stringify({
              token: 'fake',
              roles: ['ROLE_OPERADOR_GATE']
            })
          }
        ]
      }
    ]
  }
});

test.describe('Fluxos principais do Gate', () => {
  test('Agendamentos consolidados devem exibir métricas de adoção', async ({ page }) => {
    await page.route('**/gate/dashboard', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          totalAgendamentos: 18,
          percentualPontualidade: 86,
          percentualNoShow: 8,
          percentualOcupacaoSlots: 72,
          tempoMedioTurnaroundMinutos: 32,
          ocupacaoPorHora: [],
          turnaroundPorDia: [],
          percentualAbandono: 6,
          percentualAbandonoAnterior: 10,
          variacaoAbandonoPercentual: 40
        })
      });
    });

    await page.route('**/gate/agendamentos', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [],
          totalElements: 0
        })
      });
    });

    await page.goto('/home/gate/agendamentos');

    await expect(page.getByText(/Métricas de adoção/i)).toBeVisible();
    await expect(page.getByText(/Queda de 40% no abandono do fluxo/i)).toBeVisible();
  });

  test('Agendamento completo e liberação manual simulados', async ({ page }) => {
    await page.route('**/gate/dashboard', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          totalAgendamentos: 0,
          percentualPontualidade: 0,
          percentualNoShow: 0,
          percentualOcupacaoSlots: 0,
          tempoMedioTurnaroundMinutos: 0,
          ocupacaoPorHora: [],
          turnaroundPorDia: [],
          percentualAbandono: 0,
          percentualAbandonoAnterior: 0,
          variacaoAbandonoPercentual: 0
        })
      });
    });

    await page.route('**/gate/agendamentos', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [],
          totalElements: 0
        })
      });
    });

    await page.goto('/home/gate/agendamentos');
    await expect(page.getByText(/Agendamentos do Gate/i)).toBeVisible();

    await page.route('**/gate/agendamentos/1/confirmar-chegada', async (route) => {
      await route.fulfill({ status: 200, body: JSON.stringify({ id: 1, codigo: 'AG-1' }) });
    });

    await page.route('**/gate/agendamentos/1/liberacao-manual', async (route) => {
      await route.fulfill({ status: 200, body: JSON.stringify({ id: 1, status: 'LIBERADO' }) });
    });
  });
});
