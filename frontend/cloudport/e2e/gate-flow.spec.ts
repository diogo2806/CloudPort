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
  test('Dashboard deve carregar cards principais', async ({ page }) => {
    await page.route('**/gate/dashboard', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          totalAgendamentos: 5,
          percentualPontualidade: 80,
          percentualNoShow: 5,
          percentualOcupacaoSlots: 70,
          tempoMedioTurnaroundMinutos: 35,
          ocupacaoPorHora: [],
          turnaroundPorDia: []
        })
      });
    });

    await page.goto('/home/gate/dashboard');

    await expect(page.getByText(/Dashboard do Gate/i)).toBeVisible();
  });

  test('Agendamento completo e liberação manual simulados', async ({ page }) => {
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
