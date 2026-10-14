package org.kali.checkout.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record CreateOrderRequest(
        @NotBlank(message = "produto_id é obrigatório") @JsonProperty("produto_id") String produtoId,
        @NotNull(message = "quantidade é obrigatória") @Positive(message = "quantidade deve ser maior que zero") @JsonProperty("quantidade") Integer quantidade,
        @NotBlank(message = "cliente_cpf é obrigatório") @Pattern(regexp = "^$|\\d{11}", message = "cliente_cpf deve conter 11 dígitos") @JsonProperty("cliente_cpf") String clienteCpf,
        @JsonProperty("cliente_nome") String clienteNome) {
}

