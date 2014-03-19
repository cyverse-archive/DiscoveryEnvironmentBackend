package org.iplantc.workflow.experiment;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.experiment.JobNameUniquenessEnsurer. This unit test class is a little different
 * from most unit test classes in that it uses a mock object to test an abstract class that is extended by the mock
 * object. The reason this is a valid unit test is because it tests concrete methods that are implemented in the
 * abstract class.
 * 
 * @author Dennis Roberts
 */
public class JobNameUniquenessEnsurerTest {

    /**
     * The job name uniqueness ensurer instance that is currently being tested.
     */
    private MockJobNameUniquenessEnsurer jobNameUniquenessEnsurer;

    /**
     * Initializes each of the unit tests.
     */
    @Before
    public void initialize() {
        jobNameUniquenessEnsurer = new MockJobNameUniquenessEnsurer();
    }

    /**
     * Verifies that the original name is returned if it is unique.
     */
    @Test
    public void shouldReturnOriginalNameIfUnique() {
        assertEquals("foo", jobNameUniquenessEnsurer.ensureUniqueJobName("nobody", "foo"));
    }

    /**
     * Verifies that the minimum index is used if there is only one matching name.
     */
    @Test
    public void testMinimumIndex() {
        jobNameUniquenessEnsurer.addJobName("foo");
        assertEquals("foo-1", jobNameUniquenessEnsurer.ensureUniqueJobName("nobody", "foo"));
    }

    /**
     * Verifies that we can still get a unique name if there are multiple matching names.
     */
    @Test
    public void testMultipleMatchingNames() {
        jobNameUniquenessEnsurer.addJobNames(Arrays.asList("foo", "foo-1", "foo-2", "foo-3"));
        assertEquals("foo-4", jobNameUniquenessEnsurer.ensureUniqueJobName("nobody", "foo"));
    }

    /**
     * Verifies that the expected index is used if there's a gap in the indices for other unique names.
     */
    @Test
    public void testGapInMatchingNameIndices() {
        jobNameUniquenessEnsurer.addJobNames(Arrays.asList("foo", "foo-1", "foo-2", "foo-27"));
        assertEquals("foo-28", jobNameUniquenessEnsurer.ensureUniqueJobName("nobody", "foo"));
    }

    /**
     * Verifies that the expected index is used if there are names that almost match, but have extra characters at
     * the end.
     */
    @Test
    public void testNamesWithJunkAtTheEnd() {
        jobNameUniquenessEnsurer.addJobNames(Arrays.asList("foo", "foo-1-bar", "foo-2-2", "foo-27-baz"));
        assertEquals("foo-1", jobNameUniquenessEnsurer.ensureUniqueJobName("nobody", "foo"));
    }
}
