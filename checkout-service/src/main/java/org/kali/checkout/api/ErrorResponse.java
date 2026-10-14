package org.kali.checkout.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ErrorResponse(@JsonProperty("mensagem") String mensagem) {
}
