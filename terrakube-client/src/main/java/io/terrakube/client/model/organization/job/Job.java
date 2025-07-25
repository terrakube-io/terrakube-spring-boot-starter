package io.terrakube.client.model.organization.job;

import lombok.Getter;
import lombok.Setter;
import io.terrakube.client.model.generic.Resource;

@Getter
@Setter
public class Job extends Resource {
    JobAttributes attributes;
    Relationships relationships;
}
