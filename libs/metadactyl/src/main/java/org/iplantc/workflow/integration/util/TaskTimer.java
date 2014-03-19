package org.iplantc.workflow.integration.util;

import java.util.concurrent.Callable;
import org.apache.log4j.Logger;
import org.iplantc.workflow.WorkflowException;
import org.json.JSONException;

/**
 * A simple wrapper that can be used to log the amount of time a task took.
 * 
 * @author Dennis Roberts
 */
public class TaskTimer {

    /**
     * Used to log the amount of time that the operation took.
     */
    private static final Logger LOG = Logger.getLogger(TaskTimer.class);
    
    /**
     * A description of the task that is being performed.
     */
    private String description;

    /**
     * @param description a description of the task to perform.
     */
    public TaskTimer(String description) {
        this.description = description;
    }

    /**
     * Times a task that is being performed.
     * @param <T> the return type of the task.
     * @param task the task to perform.
     * @return the value returned by the task.
     */
    public <T> T time(Callable<T> task) throws JSONException {
        long start = System.nanoTime();
        long end;
        try {
            return task.call();
        }
        catch (JSONException e) {
            throw e;
        }
        catch (WorkflowException e) {
            throw e;
        }
        catch (Exception e) {
            throw new WorkflowException(e);
        }
        finally {
            end = System.nanoTime();
            double duration = (double) ((end - start) / Math.pow(10, 9));
            LOG.warn(description + " took " + duration + " seconds");
        }
    }
}
