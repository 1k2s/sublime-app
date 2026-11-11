import { createBrowserRouter } from 'react-router-dom';
import { HomeRedirect } from '@/domain/auth/components/HomeRedirect';
import { ProtectedRoute } from '@/domain/auth/components/ProtectedRoute';
import { ROLES } from '@/domain/auth/roles';
import { RouteError } from '@/shared/components/RouteError';
import { PageLoader } from '@/shared/components/PageLoader';

const ADMIN = ROLES.ADMIN.value;
const PROVIDER = ROLES.PROVIDER.value;

// Rotas por papel: cada grupo declara quem entra (ProtectedRoute com roles); quem não tem
// o papel vai para a tela inicial dele. Só experiência de uso: quem protege é a API (403)

export const router = createBrowserRouter([
  {
    path: '/login',
    lazy: async () => {
      const { LoginPage } = await import('@/domain/auth/pages/LoginPage');
      return { Component: LoginPage };
    },
    errorElement: <RouteError />
  },
  {
    element: <ProtectedRoute />,
    errorElement: <RouteError />,
    children: [
      {
        path: '/',
        lazy: async () => {
          const { AppLayout } = await import('@/app/layout/AppLayout');
          return { Component: AppLayout };
        },
        HydrateFallback: PageLoader,
        children: [
          {
            index: true,
            element: <HomeRedirect />
          },
          // Só ADMIN
          {
            element: <ProtectedRoute roles={[ADMIN]} />,
            children: [
              {
                path: 'patients',
                lazy: async () => {
                  const { PatientPage } = await import('@/domain/patient/pages/PatientPage');
                  return { Component: PatientPage };
                }
              },
              {
                path: 'pricing',
                lazy: async () => {
                  const { PricingCatalogPage } = await import('@/domain/pricing/pages/PricingCatalogPage');
                  return { Component: PricingCatalogPage };
                }
              },
              {
                path: 'providers',
                lazy: async () => {
                  const { ProviderPage } = await import('@/domain/provider/pages/ProviderPage');
                  return { Component: ProviderPage };
                }
              },
              {
                path: 'contracts',
                lazy: async () => {
                  const { ContractPage } = await import('@/domain/contract/pages/ContractPage');
                  return { Component: ContractPage };
                }
              }
            ]
          },
          // ADMIN e PROVIDER
          {
            element: <ProtectedRoute roles={[ADMIN, PROVIDER]} />,
            children: [
              {
                path: 'consultations',
                lazy: async () => {
                  const { ConsultationPage } = await import('@/domain/consultation/pages/ConsultationPage');
                  return { Component: ConsultationPage };
                }
              },
              {
                path: 'settings',
                lazy: async () => {
                  const { SettingsPage } = await import('@/domain/user/pages/SettingsPage');
                  return { Component: SettingsPage };
                }
              }
            ]
          }
        ]
      }
    ]
  },
  {
    path: '*',
    lazy: async () => {
      const { NotFoundPage } = await import('@/app/NotFoundPage');
      return { Component: NotFoundPage };
    }
  }
]);
