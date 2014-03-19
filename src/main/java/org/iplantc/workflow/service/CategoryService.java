package org.iplantc.workflow.service;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.iplantc.hibernate.util.SessionTask;
import org.iplantc.hibernate.util.SessionTaskWrapper;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.dao.TemplateGroupDao;
import org.iplantc.workflow.dao.hibernate.HibernateDaoFactory;
import org.iplantc.workflow.template.groups.TemplateGroup;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Kris Healy <healyk@iplantcollaborative.org>
 */
public class CategoryService {
    private SessionFactory sessionFactory;
    
    public CategoryService() {
        
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
    
    /**
     * Deletes categories from a list of categories.  The json input should
     * look like this:
     * 
     * <code>
     *  { "category_ids": [ "category_id_1", "category_id_2" ] }
     * </code>
     *
     *
     * 
     * @param jsonString
     *  Json input string as shown above.
     * @return 
     *  Json result
     */
    public String deleteCategories(final String jsonString) {       
        return new SessionTaskWrapper(sessionFactory).performTask(new SessionTask<String>() {
            @Override
            public String perform(Session session) {
                try {
                    JSONObject input = new JSONObject(jsonString);
                
                    String result = deleteCategories(session, input);
                    return result;
                } catch(JSONException jsonException) {
                    throw new RuntimeException("Something went horribly wrong...", jsonException);
                }
            }
        });
    }
    
    private String deleteCategories(Session session, JSONObject input) throws JSONException {
        JSONArray categories = input.getJSONArray("category_ids");
        
        DaoFactory daoFactory = new HibernateDaoFactory(session);
        TemplateGroupDao templateGroupDao = daoFactory.getTemplateGroupDao();
        
        JSONArray failures = new JSONArray();
        
        for(int i = 0; i < categories.length(); i++) {
            String id = categories.getString(i);
            TemplateGroup templateGroup = templateGroupDao.findById(id);
            
            if(templateGroup != null) {  
                // Delete all of the subgroups this template contains.
                deleteSubgroups(templateGroup, templateGroupDao);
                
                // Remove this template from other groups that may contain it
                removeTemplateFromParents(templateGroup, templateGroupDao);
                
                // Remove the template from the database
                templateGroupDao.delete(templateGroup);
            }
            
            // Failed to retreive the template group - record it as a failure.
            else {
                failures.put(id);
            }
        }
        
        JSONObject result = new JSONObject();
        result.put("failures", failures);
        
        return result.toString();
    }

    private void removeTemplateFromParents(TemplateGroup templateGroup, TemplateGroupDao templateGroupDao) {
        // Finally remove this group from any other groups 
        List<TemplateGroup> parentGroups = templateGroupDao.findTemplateGroupContainingSubgroup(templateGroup);
        for (TemplateGroup parentGroup : parentGroups) {
            parentGroup.getSub_groups().remove(templateGroup);
            templateGroupDao.save(parentGroup);
        }
    }

    private void deleteSubgroups(TemplateGroup templateGroup, TemplateGroupDao templateGroupDao) {
        // Delete subgroups
        List<TemplateGroup> subgroups = new ArrayList<TemplateGroup>(templateGroup.getSub_groups());
        templateGroup.getSub_groups().clear();

        for (TemplateGroup subgroup : subgroups) {
            if(subgroup != null) {
                templateGroupDao.delete(subgroup);
            }
        }
    }
}
