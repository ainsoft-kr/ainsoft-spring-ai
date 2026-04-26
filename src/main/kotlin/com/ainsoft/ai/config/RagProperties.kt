package com.ainsoft.ai.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Path

@ConfigurationProperties(prefix = "rag")
class RagProperties {
    var storePath: Path = Path.of("./data/rag/vector-store.json")
    var documentIndexPath: Path = Path.of("./data/rag/documents.json")
    var autoSave: Boolean = true
}
