package org.iplantc.workflow.dao.mock;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.NotificationSetDao;
import org.iplantc.workflow.template.notifications.NotificationSet;

/**
 * A mock notification set DAO used for testing.
 * 
 * @author Dennis Roberts
 */
public class MockNotificationSetDao extends MockObjectDao<NotificationSet> implements NotificationSetDao {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NotificationSet> findNotificationSetsForAnalysis(TransformationActivity analysis) {
        return findNotificationSetsForAnalysisId(analysis.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NotificationSet> findNotificationSetsForAnalysisId(String analysisId) {
        List<NotificationSet> matchingNotificationSets = new LinkedList<NotificationSet>();
        for (NotificationSet currentNotificationSet : getSavedObjects()) {
            if (StringUtils.equals(currentNotificationSet.getTemplate_id(), analysisId)) {
                matchingNotificationSets.add(currentNotificationSet);
            }
        }
        return matchingNotificationSets;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteNotificationSetsForAnalysis(TransformationActivity analysis) {
        deleteNotificationSetsForAnalysisId(analysis.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteNotificationSetsForAnalysisId(String analysisId) {
        deleteAll(findNotificationSetsForAnalysisId(analysisId));
    }
}
