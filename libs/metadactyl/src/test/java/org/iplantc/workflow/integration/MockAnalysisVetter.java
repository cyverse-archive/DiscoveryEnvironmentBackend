package org.iplantc.workflow.integration;

import org.iplantc.workflow.core.TransformationActivity;

/**
 * Mocks the functionality to vet an Analysis.  Use <code>setObjectVetted</code>
 * to determine if this will return true or false for vetting Analysis.
 * 
 * @author Kris Healy <healyk@iplantcollaborative.org>
 */
public class MockAnalysisVetter implements ObjectVetter<TransformationActivity> {
    private boolean vetResult;

    public MockAnalysisVetter() {
        vetResult = true;
    }

    /** @{inheritDocs} */
    @Override
    public boolean isObjectVetted(String username, TransformationActivity obj) {
        return vetResult;
    }

    /**
     * Sets what isObjectVetted will return.
     * 
     * @param result 
     */
    public void setObjectVetted(boolean result) {
        this.vetResult = result;
    }
}
