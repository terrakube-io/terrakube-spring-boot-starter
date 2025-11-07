package io.terrakube.client.model.graphql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphQLRequest {
    private String query;
    private Map<String, String> variables;
}
