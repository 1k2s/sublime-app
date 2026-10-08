package br.com.senai.sublime_app.shared.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import tools.jackson.databind.exc.MismatchedInputException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

// Traduz exceções de domínio para respostas HTTP no formato ProblemDetail (RFC 7807).
// Sem este handler, qualquer exceção lançada pelos services vira HTTP 500.
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Última barreira: constraint do banco violada (unique, FK) que escapou das
    // checagens prévias dos services. Ex: excluir um usuário ainda vinculado a um prestador.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "A operação viola uma restrição de integridade dos dados.");
    }

    // Erros de Bean Validation nos DTOs: devolve um mapa campo -> mensagem
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Campos da requisição inválidos.");
        problem.setProperty("errors", errors);
        return problem;
    }

    // Corpo que nem chega a virar DTO: o Jackson falha ao converter o JSON (enum
    // inexistente, tipo errado, data fora do formato, JSON malformado ou ausente).
    // Acontece antes do @Valid, por isso não cai no handleValidation.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, describeUnreadableBody(ex));
    }

    // Parâmetro da URL com tipo errado (ex: /api/patients/abc, quando o id é número)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Valor inválido para o parâmetro '" + ex.getName() + "': " + ex.getValue() + ".");
    }

    // Quando o Jackson sabe qual campo falhou, a mensagem aponta o campo (e, para
    // enum e data, o que é aceito). Sem campo identificável, o problema é o JSON
    // em si (malformado ou ausente).
    private static String describeUnreadableBody(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
            String field = fieldPath(mismatch);
            Class<?> targetType = mismatch.getTargetType();
            if (targetType != null && targetType.isEnum()) {
                return "Valor inválido para o campo '" + field + "'. Valores aceitos: " + acceptedValues(targetType) + ".";
            }
            if (targetType == LocalDate.class) {
                return "Data inválida no campo '" + field + "'. Use o formato AAAA-MM-DD.";
            }
            return "Valor inválido para o campo '" + field + "'.";
        }
        return "O corpo da requisição está ausente ou não é um JSON válido.";
    }

    // Caminho no formato do JSON: "address.cep" para campo aninhado, "[0]" para item de lista
    private static String fieldPath(MismatchedInputException ex) {
        return ex.getPath().stream()
                .map(ref -> ref.getPropertyName() != null ? ref.getPropertyName() : "[" + ref.getIndex() + "]")
                .collect(Collectors.joining("."));
    }

    private static String acceptedValues(Class<?> enumType) {
        return Arrays.stream(enumType.getEnumConstants())
                .map(Object::toString)
                .collect(Collectors.joining(", "));
    }
}
