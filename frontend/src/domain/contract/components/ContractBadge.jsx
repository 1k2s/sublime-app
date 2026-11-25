// O Badge do módulo de contrato: retângulo com cantos arredondados, no lugar da pílula. Um lugar só
// para o formato, para as etiquetas do módulo (estado, aditivo, versão, itens) ficarem iguais
import { Badge } from "@/shared/components/Badge";

export function ContractBadge(props) {
  return <Badge shape="rounded" {...props} />;
}
