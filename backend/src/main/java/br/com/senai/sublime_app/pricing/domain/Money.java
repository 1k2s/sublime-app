package br.com.senai.sublime_app.pricing.domain;

import java.math.BigDecimal;

import br.com.senai.sublime_app.shared.exception.BusinessRuleException;

// Value Object de valor em reais. Record porque é persistido por AttributeConverter
// (MoneyConverter): o Hibernate nunca instancia o Money, então ele não precisa de
// construtor vazio nem de campos mutáveis, como o Address (@Embeddable) precisa.
// O construtor é a única porta de entrada: todo Money existente já passou pelas regras.
public record Money(BigDecimal amount) {

    private static final int SCALE = 2;

    // Construtor compacto: roda antes de o record atribuir o campo. Reatribuir
    // "amount" aqui muda o valor que será guardado (a versão normalizada).
    public Money {
        validateAmount(amount);
        amount = amount.setScale(SCALE);
    }


    //Fabrica que chama o constructor. Facilita a leitura comparado com "new Money(...)""
    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    // Zero é válido (ex: repasse de atendimento que não conta); "maior que zero" é
    // regra de quem usa o Money (ex: linha de preço), não do valor em si.
    // Mais de 2 casas é rejeitado em vez de arredondado: arredondar em silêncio
    // esconderia um valor digitado errado.
    private static void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new BusinessRuleException("O valor é obrigatório.");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("O valor não pode ser negativo.");
        }
        if (amount.stripTrailingZeros().scale() > SCALE) {
            throw new BusinessRuleException("O valor deve ter no máximo " + SCALE + " casas decimais.");
        }
    }
}
