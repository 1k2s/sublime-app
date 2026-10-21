# 0011 — Contrato versionado por aditivos

- **Status:** aceito
- **Data:** 2026-10-20 (registro retroativo)

## Contexto

O contrato é um compromisso assinado: cada alteração será impressa para nova
assinatura, e o histórico completo precisa ser preservado (mesma lógica da
historização de preços, [ADR 0003](0003-preco-historizado.md)). Alterações comuns:
prorrogar um contrato vencido, incluir ou remover um grupo, trocar a frequência.

## Decisão

- **O contrato nunca é editado no lugar.** Cada alteração gera uma nova versão
  (`POST /api/contracts/{id}/amendments`) com `previousContractId` apontando para a
  versão atual, que é inativada. `previousContractId` é `unique`: cada versão tem
  no máximo uma sucessora.
- O request traz o **estado completo** da nova versão, que passa pelas mesmas
  regras da criação, exceto o fim.
- **Titular, início e plano nunca mudam** (são copiados). Trocar qualquer um deles
  é um contrato novo. Sem exceção para o plano, nem no contrato só com Grupo que
  precise de item por duração.
- **Itens podem ser incluídos e removidos**, mas o contrato não pode ficar sem
  itens.
- **O que é igual à versão anterior é aceito como está; o que é novo precisa ser
  válido** (linha de preço vigente, técnica e beneficiário ativos).
- **O fim:** por padrão mantém o atual; o request pode trazer um `endDate` só para
  estender. Incluir um item por duração recalcula: novo fim = o maior entre o fim
  atual e (data do aditivo + prazo do item − 1 dia). Remover item ou mudar a
  frequência não muda o fim.
- Só a versão **ativa** pode ser alterada (ativa porém vencida pode: é assim que se
  prorroga).

## Alternativas descartadas

- **Editar no lugar:** perderia o que foi assinado em cada versão.
- **Permitir trocar o plano:** muda toda a forma de cobrança; é encerrar e fazer
  outro contrato.
- **Recalcular o fim ao mudar a frequência, a partir do início original:** dá
  errado. Evolução quinzenal com 4 sessões usadas em 8 semanas passa a semanal → a
  conta refeita dá 12 semanas do início, e sobram 8 sessões para 4 semanas.
- **Recalcular o fim pelas sessões restantes:** exigiria o `contract` consultar o
  `consultation`, invertendo a dependência entre módulos
  ([ADR 0001](0001-monolito-modular.md)).
- **Prorrogação com prazo calculado:** prorrogação é exceção (doença, viagem), sem
  conta que preveja o tempo necessário. O admin informa o novo fim livremente,
  desde que depois do atual.

## Consequências

- O número da versão (1, 2, 3…) não é persistido: é derivável pela cadeia.
- Atendimentos de um mesmo acordo ficam distribuídos entre as versões; o saldo de
  sessões precisa somar a cadeia inteira.
- Se precisar de mais prazo ao mudar a frequência, o admin estende no mesmo
  aditivo; a tela mostra o saldo de sessões para ele decidir.
- As sessões restantes de um item removido vão para o saldo do paciente (módulo
  futuro).
