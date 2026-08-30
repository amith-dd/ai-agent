package com.example.demo.rag;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "assistant.gemini")
public class GeminiEmbeddingProperties {

    private String embeddingModel;
    private Integer embeddingOutputDimensions;

    public String getEmbeddingModel() {
        if (embeddingModel == null || embeddingModel.isBlank()) {
            return "gemini-embedding-001";
        }
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public Integer getEmbeddingOutputDimensions() {
        if (embeddingOutputDimensions == null) {
            return 768;
        }
        return embeddingOutputDimensions;
    }

    public void setEmbeddingOutputDimensions(Integer embeddingOutputDimensions) {
        this.embeddingOutputDimensions = embeddingOutputDimensions;
    }
}
