package org.iplantc.workflow.experiment;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Used to ensure that job names are unique for a given user.
 * 
 * @author Dennis Roberts
 */
public abstract class JobNameUniquenessEnsurer {

    /**
     * Ensures that the given job name is unique for the given user.
     * 
     * @param username the name of the user who is submitting the job.
     * @param jobName the job name requested by the user.
     * @return the updated job name.
     */
    public String ensureUniqueJobName(String username, String jobName) {
        List<String> matchingNames = findMatchingNames(username, jobName);
        return matchingNames.isEmpty() ? jobName : findUniqueJobName(matchingNames, jobName);
    }

    /**
     * Finds a unique job name for the given job name.
     * 
     * @param matchingNames the list of matching names.
     * @param jobName the requested job name.
     * @return the unique job name.
     */
    private String findUniqueJobName(List<String> matchingNames, String jobName) {
        int index = findUniqueIndex(matchingNames, jobName);
        return jobName + "-" + index;
    }

    /**
     * Finds a unique index for the given job name.
     * 
     * @param matchingNames the list of matching names.
     * @param jobName the requested job name.
     * @return a unique index that can be appended to the job name.
     */
    private int findUniqueIndex(List<String> matchingNames, String jobName) {
        int index = 0;
        Pattern pattern = Pattern.compile("\\A\\Q" + jobName + "\\E-(\\d+)\\z");
        for (String name : matchingNames) {
            Matcher matcher = pattern.matcher(name);
            if (matcher.matches()) {
                index = Math.max(index, Integer.parseInt(matcher.group(1)));
            }
        }
        return index + 1;
    }

    /**
     * Finds matching job names for the given username and job name.
     * 
     * @param username the name of the user who is submitting the job.
     * @param jobName the requested job name.
     * @return a list of matching job names.
     */
    protected abstract List<String> findMatchingNames(String username, String jobName);
}
