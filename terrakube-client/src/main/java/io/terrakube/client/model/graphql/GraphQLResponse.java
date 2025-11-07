package io.terrakube.client.model.graphql;

import lombok.Data;
import java.util.Map;

@Data
public class GraphQLResponse<T> {
    private T data;
    private Map<String, Object> errors;
}
