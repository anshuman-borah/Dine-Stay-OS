package project.EnterpriseSaas.demo.common.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LenientMapDeserializer extends JsonDeserializer<Map<String, Object>> {

    @Override
    public Map<String, Object> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken token = p.currentToken();

        // If the database has `[]` (array), safely return an empty Map
        if (token == JsonToken.START_ARRAY) {
            p.skipChildren();
            return new HashMap<>();
        }

        if (token == JsonToken.VALUE_NULL) {
            return new HashMap<>();
        }

        if (token == JsonToken.START_OBJECT) {
            return p.readValueAs(new TypeReference<HashMap<String, Object>>() {});
        }

        return new HashMap<>();
    }
}