package org.iplantc.workflow.dao;

import java.util.List;

import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.template.notifications.NotificationSet;

/**
 * Used to access persistent notification sets.
 * 
 * @author Dennis Roberts
 */
public interface NotificationSetDao extends GenericObjectDao<NotificationSet> {

    /**
     * Finds all of the notification sets associated with the given analysis.
     * 
     * @param analysis the analysis.
     * @return the list of notification sets.
     */
    public List<NotificationSet> findNotificationSetsForAnalysis(TransformationActivity analysis);

    /**
     * Finds all of the notification sets associated with the given analysis ID.
     * 
     * @param analysisId the analysis identifier.
     * @return the list of notification sets.
     */
    public List<NotificationSet> findNotificationSetsForAnalysisId(String analysisId);

    /**
     * Deletes all of the notification sets associated with the given analysis.
     * 
     * @param analysis the analysis.
     */
    public void deleteNotificationSetsForAnalysis(TransformationActivity analysis);

    /**
     * Deletes all of the notification sets associated with the given analysis identifier.
     * 
     * @param analysisId the analysis identifier.
     */
    public void deleteNotificationSetsForAnalysisId(String analysisId);
}
