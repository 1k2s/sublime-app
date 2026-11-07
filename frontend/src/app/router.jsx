import { createBrowserRouter } from 'react-router-dom';
import { HomeRedirect } from '@/domain/auth/components/HomeRedirect';
import { ProtectedRoute } from '@/domain/auth/components/ProtectedRoute';
import { RouteError } from '@/shared/ui/RouteError';
import { PageLoader } from '@/shared/ui/PageLoader';

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
            path: 'users',
            lazy: async () => {
              const { UserPage } = await import('@/domain/user/pages/UserPage');
              return { Component: UserPage };
            }
          },
          {
            path: 'contracts',
            lazy: async () => {
              const { ContractPage } = await import('@/domain/contract/pages/ContractPage');
              return { Component: ContractPage };
            }
          },
          {
            path: 'consultations',
            lazy: async () => {
              const { ConsultationPage } = await import('@/domain/consultation/pages/ConsultationPage');
              return { Component: ConsultationPage };
            }
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
