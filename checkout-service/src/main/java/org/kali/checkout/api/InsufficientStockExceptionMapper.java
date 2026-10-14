package org.kali.checkout.api;

import org.kali.checkout.application.InsufficientStockException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InsufficientStockExceptionMapper implements ExceptionMapper<InsufficientStockException> {

    @Override
    public Response toResponse(InsufficientStockException e) {
        return Response.status(Response.Status.CONFLICT).type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(e.getMessage())).build();
    }
}

