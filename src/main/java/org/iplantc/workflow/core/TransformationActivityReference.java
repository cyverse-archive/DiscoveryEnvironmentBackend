package org.iplantc.workflow.core;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 *
 * @author Kris Healy <healyk@iplantcollaborative.org>
 */
@Entity
@Table(name = "transformation_activity_references")
public class TransformationActivityReference implements Serializable {
    private long id;
    private TransformationActivity transformationActivity;
    private String referenceText;
    
    public TransformationActivityReference() {
        
    }

    @SequenceGenerator(name="transformation_activity_references_id_seq", sequenceName="transformation_activity_references_id_seq")
	@GeneratedValue(generator="transformation_activity_references_id_seq")
    @Id
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Column(name = "reference_text", nullable = false)
    public String getReferenceText() {
        return referenceText;
    }

    public void setReferenceText(String referenceText) {
        this.referenceText = referenceText;
    }

    @ManyToOne
    @JoinColumn(name = "transformation_activity_id", insertable = false, updatable = false)
    public TransformationActivity getTransformationActivity() {
        return transformationActivity;
    }

    public void setTransformationActivity(TransformationActivity transformationActivity) {
        this.transformationActivity = transformationActivity;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TransformationActivityReference other = (TransformationActivityReference) obj;
        if (this.transformationActivity != other.transformationActivity && (this.transformationActivity == null || !this.transformationActivity.equals(other.transformationActivity))) {
            return false;
        }
        if ((this.referenceText == null) ? (other.referenceText != null) : !this.referenceText.equals(other.referenceText)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + (this.transformationActivity != null ? this.transformationActivity.hashCode() : 0);
        hash = 67 * hash + (this.referenceText != null ? this.referenceText.hashCode() : 0);
        return hash;
    }
}
