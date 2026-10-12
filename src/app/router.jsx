import { createBrowserRouter, Navigate } from 'react-router-dom';
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
    path: '/',
    lazy: async () => {
      const { AppLayout } = await import('@/app/layout/AppLayout');
      return { Component: AppLayout };
    },
    errorElement: <RouteError />,
    HydrateFallback: PageLoader,
    children: [
      {
        index: true,
        element: <Navigate to="/consultations" replace />
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
  },
  {
    path: '*',
    lazy: async () => {
      const { NotFoundPage } = await import('@/pages/NotFoundPage');
      return { Component: NotFoundPage };
    }
  }
]);
