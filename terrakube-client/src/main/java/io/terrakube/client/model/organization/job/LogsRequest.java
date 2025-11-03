package io.terrakube.client.model.organization.job;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LogsRequest {
    List<Log> data;
}