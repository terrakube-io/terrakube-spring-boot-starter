package io.terrakube.client.model.graphql.queries.search.provider;

import lombok.Data;

@Data
public class Implementation {
    private String id;
    private String os;
    private String arch;
    private String filename;
    private String downloadUrl;
    private String shasumsUrl;
    private String shasumsSignatureUrl;
    private String shasum;
    private String keyId;
    private String asciiArmor;
    private String trustSignature;
    private String source;
    private String sourceUrl;
}