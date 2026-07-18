package org.salvationarmy.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BulkProxyRegisterRequest {

    @NotNull
    @Valid
    @JsonProperty("proxy_user")
    private ProxyUserPayload proxyUser;

    @NotEmpty
    @Valid
    private List<DependentPayload> dependents;
}
