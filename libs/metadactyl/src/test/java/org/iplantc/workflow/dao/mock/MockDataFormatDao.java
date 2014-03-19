package org.iplantc.workflow.dao.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.iplantc.persistence.dto.data.DataFormat;
import org.iplantc.workflow.dao.DataFormatDao;

/**
 * Used to access persistent data formats.
 * 
 * @author Dennis Roberts
 */
public class MockDataFormatDao implements DataFormatDao {
    private HashMap<String, DataFormat> store;
    
    public MockDataFormatDao() {
        store = new HashMap<String, DataFormat>();
    }
    
    @Override
    public DataFormat findByName(String name) {
        for(String key : store.keySet()) {
            DataFormat dataFormat = store.get(key);
            
            if(dataFormat.getName().equals(name)) {
                return dataFormat;
            }
        }
        
        return null;
    }

    @Override
    public void save(DataFormat object) {
        store.put(object.getGuid(), object);
    }

    @Override
    public void delete(DataFormat object) {
        store.remove(object.getGuid());
    }

    @Override
    public void deleteAll(Collection<DataFormat> objects) {
        store.clear();
    }

    @Override
    public void deleteById(long id) {
        DataFormat obj = findById(id);
        if(obj != null) {
            store.remove(obj.getGuid());
        }
    }

    @Override
    public DataFormat findById(long id) {
        for(String key : store.keySet()) {
            DataFormat dataFormat = store.get(key);
            
            if(dataFormat.getId() == id) {
                return dataFormat;
            }
        }
        
        return null;
    }

    @Override
    public List<DataFormat> findAll() {
        return new ArrayList<DataFormat>(store.values());
    }
}
