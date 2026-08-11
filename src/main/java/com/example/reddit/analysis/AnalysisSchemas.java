package com.example.reddit.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;

@ApplicationScoped
public class AnalysisSchemas {
    private final JsonNode chunkSchema;
    private final JsonNode resultSchema;

    public AnalysisSchemas(ObjectMapper objectMapper) {
        try {
            chunkSchema = objectMapper.readTree(CHUNK_SCHEMA);
            resultSchema = objectMapper.readTree(RESULT_SCHEMA);
        } catch (IOException exception) {
            throw new IllegalStateException("Invalid built-in analysis JSON schema", exception);
        }
    }

    public JsonNode chunkSchema() {
        return chunkSchema.deepCopy();
    }

    public JsonNode resultSchema() {
        return resultSchema.deepCopy();
    }

    private static final String TOPIC_PROPERTIES = """
            "name":{"type":"string"},
            "summary":{"type":"string"},
            "sentiment":{"type":"string","enum":["POSITIVE","NEGATIVE","NEUTRAL","MIXED"]},
            "sentimentScore":{"type":"number"},
            "mentionCount":{"type":"integer"}
            """;

    private static final String EVIDENCE_SCHEMA = """
            {
              "type":"object",
              "additionalProperties":false,
              "properties":{
                "sourceType":{"type":"string","enum":["POST","COMMENT"]},
                "sourceId":{"type":"integer"},
                "stance":{"type":"string","enum":["SUPPORTS","CONTRADICTS","CONTEXT"]},
                "excerpt":{"type":"string"},
                "rationale":{"type":"string"}
              },
              "required":["sourceType","sourceId","stance","excerpt","rationale"]
            }
            """;

    private static final String CLAIM_SCHEMA = """
            {
              "type":"object",
              "additionalProperties":false,
              "properties":{
                "topic":{"type":"string"},
                "text":{"type":"string"},
                "type":{"type":"string","enum":["EXPERIENCE","OPINION","FACTUAL_ASSERTION","RECOMMENDATION"]},
                "sentiment":{"type":"string","enum":["POSITIVE","NEGATIVE","NEUTRAL","MIXED"]},
                "confidence":{"type":"number"},
                "evidence":{"type":"array","items":%s}
              },
              "required":["topic","text","type","sentiment","confidence","evidence"]
            }
            """.formatted(EVIDENCE_SCHEMA);

    private static final String CHUNK_SCHEMA = """
            {
              "type":"object",
              "additionalProperties":false,
              "properties":{
                "topics":{"type":"array","items":{
                  "type":"object","additionalProperties":false,
                  "properties":{%s},
                  "required":["name","summary","sentiment","sentimentScore","mentionCount"]
                }},
                "claims":{"type":"array","items":%s}
              },
              "required":["topics","claims"]
            }
            """.formatted(TOPIC_PROPERTIES, CLAIM_SCHEMA);

    private static final String RESULT_SCHEMA = """
            {
              "type":"object",
              "additionalProperties":false,
              "properties":{
                "topics":{"type":"array","items":{
                  "type":"object","additionalProperties":false,
                  "properties":{%s},
                  "required":["name","summary","sentiment","sentimentScore","mentionCount"]
                }},
                "claims":{"type":"array","items":%s},
                "report":{
                  "type":"object",
                  "additionalProperties":false,
                  "properties":{
                    "executiveSummary":{"type":"string"},
                    "keyFindings":{"type":"array","items":{"type":"string"}},
                    "opportunities":{"type":"array","items":{"type":"string"}},
                    "risks":{"type":"array","items":{"type":"string"}},
                    "recommendations":{"type":"array","items":{"type":"string"}},
                    "limitations":{"type":"array","items":{"type":"string"}}
                  },
                  "required":["executiveSummary","keyFindings","opportunities","risks","recommendations","limitations"]
                }
              },
              "required":["topics","claims","report"]
            }
            """.formatted(TOPIC_PROPERTIES, CLAIM_SCHEMA);
}
