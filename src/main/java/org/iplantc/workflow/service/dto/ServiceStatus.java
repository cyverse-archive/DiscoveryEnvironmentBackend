package org.iplantc.workflow.service.dto;

/**
 * A data transfer object used to indicate whether or not a service succeeded.
 * 
 * @author Dennis Roberts
 */
public class ServiceStatus extends AbstractDto {

    /**
     * Indicates that a service succeeded.
     */
    public static ServiceStatus SUCCESS = new ServiceStatus(true);

    /**
     * Indicates that a service did not succeed.
     */
    public static ServiceStatus FAILURE = new ServiceStatus(false);

    /**
     * True if the service succeeded.
     */
    @JsonField(name = "success")
    protected boolean success;

    /**
     * @param success true if the service succeeded.
     */
    public ServiceStatus(boolean success) {
        this.success = success;
    }
}
