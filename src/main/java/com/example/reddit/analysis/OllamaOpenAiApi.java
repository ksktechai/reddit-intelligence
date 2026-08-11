package com.example.reddit.analysis;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/chat/completions")
@RegisterRestClient(configKey = "analysis-model")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface OllamaOpenAiApi {

    @POST
    Response chat(OllamaChatRequest request, @HeaderParam("Authorization") String authorization);
}
