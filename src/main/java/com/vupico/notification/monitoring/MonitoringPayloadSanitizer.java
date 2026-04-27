package com.vupico.notification.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.springframework.web.multipart.MultipartFile;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Monitoring-safe JSON snapshots: redact likely secrets, truncate strings, cap array sizes.
 */
public final class MonitoringPayloadSanitizer {

    private static final Pattern SENSITIVE_KEY = Pattern.compile(
            "(?i).*(password|passwd|secret|token|apikey|api[_-]?key|auth|authorization|credential|bearer|"
                    + "accesskey|access[_-]?key|secretkey|secret[_-]?key|privatekey|jwt|refresh|ssn|credit|cardnumber|smtp|"
                    + "smtp_addr|api-key|x-api-key).*");

    private static final int DEFAULT_MAX_STRING = 512;
    private static final int DEFAULT_MAX_ARRAY = 10;
    private static final int DEFAULT_MAX_DEPTH = 12;

    private MonitoringPayloadSanitizer() {
    }

    public static JsonNode sanitizeTree(ObjectMapper mapper, Object value) {
        if (value == null) {
            return JsonNodeFactory.instance.nullNode();
        }
        if (value instanceof MultipartFile file) {
            return multipartSummary(mapper, file);
        }
        try {
            JsonNode raw = mapper.valueToTree(value);
            return sanitizeNode(raw, 0);
        } catch (IllegalArgumentException e) {
            return TextNode.valueOf("[unserializable:" + value.getClass().getSimpleName() + "]");
        }
    }

    public static ObjectNode multipartSummary(ObjectMapper mapper, MultipartFile file) {
        ObjectNode n = mapper.createObjectNode();
        if (file == null) {
            n.put("file", "null");
            return n;
        }
        n.put("originalFilename", nullToEmpty(file.getOriginalFilename()));
        n.put("contentType", nullToEmpty(file.getContentType()));
        n.put("sizeBytes", file.getSize());
        n.put("empty", file.isEmpty());
        return n;
    }

    public static ObjectNode stringPayloadSummary(ObjectMapper mapper, String raw, int maxPreviewChars) {
        ObjectNode n = mapper.createObjectNode();
        if (raw == null) {
            n.put("chars", 0);
            return n;
        }
        n.put("chars", raw.length());
        int cap = Math.min(maxPreviewChars, raw.length());
        if (cap > 0) {
            String preview = raw.substring(0, cap);
            n.put("previewTruncated", preview + (raw.length() > cap ? "…" : ""));
        }
        return n;
    }

    private static JsonNode sanitizeNode(JsonNode node, int depth) {
        if (node == null || node.isNull()) {
            return node;
        }
        if (depth > DEFAULT_MAX_DEPTH) {
            return TextNode.valueOf("[max-depth]");
        }
        if (node.isTextual()) {
            return TextNode.valueOf(truncateString(node.asText(), DEFAULT_MAX_STRING));
        }
        if (node.isNumber() || node.isBoolean()) {
            return node;
        }
        if (node.isArray()) {
            ArrayNode out = JsonNodeFactory.instance.arrayNode();
            int i = 0;
            for (JsonNode item : node) {
                if (i >= DEFAULT_MAX_ARRAY) {
                    out.add(TextNode.valueOf("… " + (node.size() - DEFAULT_MAX_ARRAY) + " more items"));
                    break;
                }
                out.add(sanitizeNode(item, depth + 1));
                i++;
            }
            return out;
        }
        if (node.isObject()) {
            ObjectNode out = JsonNodeFactory.instance.objectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> e = fields.next();
                String key = e.getKey();
                if (isSensitiveKey(key)) {
                    out.put(key, "***REDACTED***");
                } else {
                    out.set(key, sanitizeNode(e.getValue(), depth + 1));
                }
            }
            return out;
        }
        return TextNode.valueOf(node.toString());
    }

    private static boolean isSensitiveKey(String key) {
        return key != null && SENSITIVE_KEY.matcher(key).matches();
    }

    private static String truncateString(String s, int max) {
        if (s == null) {
            return null;
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "…";
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
