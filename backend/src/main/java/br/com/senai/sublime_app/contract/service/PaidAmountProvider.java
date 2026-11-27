package br.com.senai.sublime_app.contract.service;

import java.util.List;

import br.com.senai.sublime_app.pricing.domain.Money;

// O pago de um contrato, que o aditivo precisa (as parcelas novas dividem o que falta pagar).
// Os pagamentos são do módulo billing, acima do contract no mapa de módulos: o contract declara
// esta interface e o billing a implementa (inversão de dependência), para a dependência continuar
// apontando para cima. Ver "Mapa de módulos" no architecture.md.
public interface PaidAmountProvider {

    // A soma dos pagamentos ativos da cadeia de versões do contrato (os ids de todas as versões)
    Money paidOn(List<Long> contractChainIds);
}
