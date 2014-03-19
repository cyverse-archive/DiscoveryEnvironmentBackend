package org.iplantc.workflow.service.dto.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.iplantc.persistence.dto.listing.AnalysisListing;
import org.iplantc.persistence.dto.listing.DeployedComponentListing;
import org.iplantc.workflow.service.dto.AbstractDto;
import org.iplantc.workflow.service.dto.JsonField;
import org.iplantc.workflow.util.Lambda;
import org.iplantc.workflow.util.ListUtils;

/**
 * A data transfer object for a list of deployed components.
 * 
 * @author Dennis Roberts
 */
public class DeployedComponentListDto extends AbstractDto {

    /**
     * The list of deployed components.
     */
    @JsonField(name = "components")
    List<DeployedComponentDto> deployedComponents = new ArrayList<DeployedComponentDto>();

    /**
     * @return an unmodifiable copy of the list of deployed components.
     */
    public List<DeployedComponentDto> getDeployedComponents() {
        return Collections.unmodifiableList(deployedComponents);
    }

    /**
     * Initializes a new deployed component DTO for an analysis.
     * 
     * @param analysis the analysis listing.
     */
    public DeployedComponentListDto(final AnalysisListing analysis) {
        deployedComponents.addAll(ListUtils.map(new Lambda<DeployedComponentListing, DeployedComponentDto>() {
            @Override
            public DeployedComponentDto call(DeployedComponentListing arg) {
                return new DeployedComponentDto(arg);
            }
        }, analysis.getDeployedComponents()));
    }
}
