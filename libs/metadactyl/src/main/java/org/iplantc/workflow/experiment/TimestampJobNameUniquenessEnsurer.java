package org.iplantc.workflow.experiment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 *
 * @author dennis
 */
public class TimestampJobNameUniquenessEnsurer extends JobNameUniquenessEnsurer {

    /**
     * Adds a timestamp to the job name in order to ensure job name uniqueness.
     *
     * @param username the name of the user submitting the job.
     * @param jobName the name of the job.
     * @return the unique job name.
     */
    @Override
    public String ensureUniqueJobName(String username, String jobName) {
        return jobName.replaceAll(" ", "_") + formatTimestamp();
    }

    /**
     * We're relying on the timestamp to ensure that the job name is unique, so it's not necessary to check existing
     * job names.
     *
     * @param username the username.
     * @param jobName the job name.
     * @return an empty list of strings.
     */
    @Override
    protected List<String> findMatchingNames(String username, String jobName) {
        return new ArrayList<String>();
    }

    /**
     * Formats the current timestamp.
     *
     * @return the formatted timestamp.
     */
    private String formatTimestamp() {
        SimpleDateFormat format = new SimpleDateFormat("-yyyy-MM-dd-HH-mm-ss.SSS");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date());
    }
}
