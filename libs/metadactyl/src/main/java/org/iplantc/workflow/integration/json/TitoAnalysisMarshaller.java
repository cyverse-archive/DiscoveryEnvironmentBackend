package org.iplantc.workflow.integration.json;

import static org.iplantc.workflow.integration.util.JsonUtils.mapToJsonObject;
import static org.iplantc.workflow.integration.util.JsonUtils.putIfNotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.iplantc.persistence.dto.step.TransformationStep;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.Rating;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.core.TransformationActivityReference;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.data.InputOutputMap;
import org.iplantc.workflow.model.Template;
import org.iplantc.workflow.template.groups.TemplateGroup;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to convert an existing analysis to a JSON document.
 * 
 * @author Dennis Roberts
 */
public class TitoAnalysisMarshaller implements TitoMarshaller<TransformationActivity> {

    /**
     * Used to obtain data access objects.
     */
    private final DaoFactory daoFactory;

    /**
     * True if the JSON produced by this marshaller should use backward references.
     */
    private final boolean useReferences;
    
    private final TitoIntegrationDatumMashaller integrationDatumMarshaller;
    private final TitoImplementationDataFileMarshaller implementationDataFileMarshaller;

    /**
     * Creates an analysis marshaller that does not produce JSON that uses backward references.  A DAO factory is not
     * required if backward references are not enabled.
     */
    public TitoAnalysisMarshaller() {
        this(null, false);
    }

    /**
     * @param daoFactory used to obtain data access objects.
     * @param useReferences true if the JSON produced by this marshaller should use backward references.
     */
    public TitoAnalysisMarshaller(DaoFactory daoFactory, boolean useReferences) {
        this.daoFactory = daoFactory;
        this.useReferences = useReferences;
        
        this.integrationDatumMarshaller = new TitoIntegrationDatumMashaller();
        this.implementationDataFileMarshaller = new TitoImplementationDataFileMarshaller();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJson(TransformationActivity object) {
        try {
            return marshalAnalysis(object);
        }
        catch (JSONException e) {
            throw new WorkflowException("error producing JSON object", e);
        }
    }

    /**
     * Marshals a single analysis.
     * 
     * @param analysis the analysis to marshal.
     * @return the marshaled analysis.
     * @throws JSONException if a JSON error occurs.
     */
    private JSONObject marshalAnalysis(TransformationActivity analysis) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("analysis_id", analysis.getId());
        json.put("analysis_name", analysis.getName());
        json.put("description", analysis.getDescription());
        json.put("type", analysis.getType());
        json.put("deleted", analysis.isDeleted());
        json.put("wiki_url", analysis.getWikiurl());
        json.put("steps", marshalSteps(analysis.getSteps()));
        putIfNotNull(json, "mappings", marshalInputOutputMappings(analysis.getMappings()));
        
        json.put("references", marshalReferences(analysis.getReferences()));
        json.put("suggested_groups", marshalGroups(analysis.getSuggestedGroups()));
        
        JSONObject implementation = integrationDatumMarshaller.toJson(analysis.getIntegrationDatum());
        json.put("implementation", implementation);
        json.put("ratings", marshalRatings(analysis.getRatings()));
        
        return json;
    }
    
    private JSONArray marshalRatings(Set<Rating> ratings) throws JSONException {
        JSONArray results = new JSONArray();
        
        if(ratings != null) {
            for (Rating rating : ratings) {
                JSONObject jsonRating = new JSONObject();

                jsonRating.put("rating", rating.getRaiting());
                jsonRating.put("username", rating.getUser().getUsername());

                results.put(jsonRating);
            }
        }
        
        return results;
    }
    
    private JSONArray marshalGroups(Collection<TemplateGroup> groups) {
        JSONArray output = new JSONArray();
        
        if(groups != null) {
            for (TemplateGroup templateGroup : groups) {
                output.put(templateGroup.getId());
            }
        }
        
        return output;
    }
    
    public JSONArray marshalReferences(Collection<TransformationActivityReference> references) {
        JSONArray output = new JSONArray();
        
        if(references != null) {
            for (TransformationActivityReference transformationActivityReference : references) {
                output.put(transformationActivityReference.getReferenceText());
            }
        }
        
        return output;
    }

    /**
     * Marshals the input/output mappings for an analysis.
     * 
     * @param mappings the input/output mappings.
     * @return the marshaled input/output mappings or null if there are no mappings.
     * @throws JSONException if a JSON error occurs.
     */
    private JSONArray marshalInputOutputMappings(List<InputOutputMap> mappings) throws JSONException {
        JSONArray array = null;
        if (mappings.size() > 0) {
            array = new JSONArray();
            for (InputOutputMap mapping : mappings) {
                array.put(marshalInputOutputMapping(mapping));
            }
        }
        return array;
    }

    /**
     * Marshals a single input/output mapping.
     * 
     * @param mapping the mapping to marshal.
     * @return the marshaled mapping.
     * @throws JSONException if a JSON error occurs.
     */
    private JSONObject marshalInputOutputMapping(InputOutputMap mapping) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("source_step", mapping.getSource().getName());
        json.put("target_step", mapping.getTarget().getName());
        json.put("map", mapToJsonObject(mapping.getInput_output_relation()));
        return json;
    }

    /**
     * Marshals the list of transformation steps.
     * 
     * @param steps the list of transformation steps.
     * @return the marshaled list of transformation steps.
     * @throws JSONException if a JSON error occurs.
     */
    private JSONArray marshalSteps(List<TransformationStep> steps) throws JSONException {
        JSONArray array = new JSONArray();
        for (TransformationStep step : steps) {
            array.put(marshalStep(step));
        }
        return array;
    }

    /**
     * Marshals a single transformation step.
     * 
     * @param step the transformation step to marshal.
     * @return the marshaled transformation step.
     * @throws JSONException if a JSON error occurs.
     */
    private JSONObject marshalStep(TransformationStep step) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", step.getGuid());
        json.put("name", step.getName());
        json.put("description", step.getDescription());
        if (useReferences) {
            json.put("template_ref", getTemplateName(step.getTransformation().getTemplate_id()));
        }
        else {
            json.put("template_id", step.getTransformation().getTemplate_id());
        }
        
        Map<String, String> propertyValues = step.getTransformation().getPropertyValues();
        JSONObject config = new JSONObject();
        for(String key : propertyValues.keySet()) {
            config.put(key, propertyValues.get(key));
        }
        
        json.put("config", config);
        return json;
    }

    /**
     * Gets the name of the template with the given identifier.
     * 
     * @param templateId the template identifier.
     * @return the template name.
     */
    private String getTemplateName(String templateId) {
        Template template = daoFactory.getTemplateDao().findById(templateId);
        if (template == null) {
            throw new WorkflowException("no template with ID, " + templateId + ", found");
        }
        return template.getName();
    }
}
