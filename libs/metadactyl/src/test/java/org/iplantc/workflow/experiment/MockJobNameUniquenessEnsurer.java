package org.iplantc.workflow.experiment;

import java.util.LinkedList;
import java.util.List;

/**
 * A mock object used to ensure job name uniqueness.
 * 
 * @author Dennis Roberts
 */
public class MockJobNameUniquenessEnsurer extends JobNameUniquenessEnsurer {

    /**
     * This list of known job names.
     */
    private List<String> knownJobNames = new LinkedList<String>();

    /**
     * Adds a job name to the list of known job names.
     * 
     * @param name the job name to add.
     */
    public void addJobName(String name) {
        knownJobNames.add(name);
    }

    /**
     * Adds multiple job names to the list of known job names.
     * 
     * @param names the job names to add.
     */
    public void addJobNames(List<String> names) {
        knownJobNames.addAll(names);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<String> findMatchingNames(String username, String jobName) {
        List<String> matchingNames = new LinkedList<String>();
        for (String currentName : knownJobNames) {
            if (currentName.startsWith(jobName)) {
                matchingNames.add(currentName);
            }
        }
        return matchingNames;
    }
}
