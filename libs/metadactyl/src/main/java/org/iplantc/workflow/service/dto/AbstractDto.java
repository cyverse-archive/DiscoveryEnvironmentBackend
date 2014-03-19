package org.iplantc.workflow.service.dto;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.iplantc.workflow.WorkflowException;

/**
 * An abstract data transfer object that automates some JSON deserialization tasks.
 * 
 * @author Dennis Roberts
 */
public abstract class AbstractDto {

    /**
     * The types supported by the JSON library.
     */
    private static final Set<Class<?>> TYPES_SUPPORTED_BY_JSON_LIB;

    /**
     * Initialize the types supported by the JSON library.
     */
    static {
        Set<Class<?>> set = new HashSet<Class<?>>();
        set.add(Boolean.class);
        set.add(Double.class);
        set.add(Integer.class);
        set.add(Long.class);
        set.add(String.class);
        TYPES_SUPPORTED_BY_JSON_LIB = Collections.unmodifiableSet(set);
    }

    /**
     * Initializes the object from the given JSON object.
     * 
     * @param json the JSON object.
     */
    protected void fromJson(JSONObject json) {
        for (Field field : getClass().getDeclaredFields()) {
            JsonField annotation = field.getAnnotation(JsonField.class);
            if (annotation != null) {
                setField(field, json, annotation);
            }
        }
    }

    /**
     * Converts this object to a JSON object.
     * 
     * @return the JSON object.
     */
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        for (Field field : getClass().getDeclaredFields()) {
            JsonField annotation = field.getAnnotation(JsonField.class);
            if (annotation != null) {
                setJsonField(field, json, annotation);
            }
        }
        return json;
    }

    /**
     * Initializes this object from a JSON string.
     * 
     * @param str the JSON string.
     */
    protected void fromString(String str) {
        fromJson((JSONObject) JSONSerializer.toJSON(str));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toJson().toString();
    }

    /**
     * Sets a JSON field using the value of an object field.
     * 
     * @param field the object field.
     * @param json the JSON object.
     * @param annotation the JsonField annotation.
     */
    private void setJsonField(Field field, JSONObject json, JsonField annotation) {
        field.setAccessible(true);
        try {
            if (fieldRequiredOrAvailable(annotation, field)) {
                if (field.getType() == Boolean.TYPE) {
                    json.put(annotation.name(), field.getBoolean(this));
                }
                else if (field.getType() == Double.TYPE) {
                    json.put(annotation.name(), field.getDouble(this));
                }
                else if (field.getType() == Integer.TYPE) {
                    json.put(annotation.name(), field.getInt(this));
                }
                else if (field.getType() == Long.TYPE) {
                    json.put(annotation.name(), field.getLong(this));
                }
                else if (typeSupportedByJsonLib(field.getType())) {
                    json.put(annotation.name(), field.get(this));
                }
                else if (AbstractDto.class.isAssignableFrom(field.getType())) {
                    json.put(annotation.name(), ((AbstractDto) field.get(this)).toJson());
                }
                else if (List.class.isAssignableFrom(field.getType())) {
                    if (field.getGenericType() instanceof ParameterizedType) {
                        ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
                        Class<?> elementType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
                        json.put(annotation.name(), jsonArrayFromList((List<?>) field.get(this), elementType));
                    }
                    else {
                        json.put(annotation.name(), jsonArrayFromList((List<?>) field.get(this), Object.class));
                    }
                }
                else {
                    throw new WorkflowException("unsupported field type: " + field.getType().getName());
                }
            }
            else if (hasDefault(annotation)) {
                json.put(annotation.name(), annotation.defaultValue());
            }
        }
        catch (IllegalAccessException e) {
            throw new WorkflowException("unable to get the value of field, " + field.getName(), e);
        }
    }

    /**
     * Determines whether or not a type is directly supported by the JSON library.  For now, the set of supported
     * types included boxed primitives and strings.
     * 
     * @param type the type to check.
     * @return true if the type is directly supported by the JSON library.
     */
    private boolean typeSupportedByJsonLib(Class<?> type) {
        return TYPES_SUPPORTED_BY_JSON_LIB.contains(type);
    }

    /**
     * Sets a field value from a field in a JSON object.
     * 
     * @param field the object field.
     * @param json the JSON object.
     * @param annotation the JsonField annotation.
     */
    private void setField(Field field, JSONObject json, JsonField annotation) {
        field.setAccessible(true);
        try {
            if (elementRequiredOrAvailable(annotation, json)) {
                if (field.getType() == Boolean.TYPE) {
                    field.setBoolean(this, json.getBoolean(annotation.name()));
                }
                else if (field.getType() == Double.TYPE) {
                    field.setDouble(this, json.getDouble(annotation.name()));
                }
                else if (field.getType() == Integer.TYPE) {
                    field.setInt(this, json.getInt(annotation.name()));
                }
                else if (field.getType() == Long.TYPE) {
                    field.setLong(this, json.getLong(annotation.name()));
                }
                else if (typeSupportedByJsonLib(field.getType())) {
                    field.set(this, field.getType().cast(json.get(annotation.name())));
                }
                else if (AbstractDto.class.isAssignableFrom(field.getType())) {
                    field.set(this, abstractDtoFromJson(field.getType(), json.getJSONObject(annotation.name())));
                }
                else if (List.class.isAssignableFrom(field.getType())) {
                    if (field.getGenericType() instanceof ParameterizedType) {
                        ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
                        Class<?> elementType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
                        field.set(this, listFromJsonArray(json.getJSONArray(annotation.name()), elementType));
                    }
                    else {
                        field.set(this, listFromJsonArray(json.getJSONArray(annotation.name()), Object.class));
                    }
                }
                else {
                    throw new WorkflowException("unsupported field type: " + field.getType().getName());
                }
            }
        }
        catch (IllegalAccessException e) {
            throw new WorkflowException("unable to set the value of field, " + field.getName(), e);
        }
    }

    /**
     * Determines if a field is either required by the DTO or available in the JSON object.
     * 
     * @param annotation the annotation representing the field.
     * @param field the field.
     * @return true if the element is either required or available.
     * @throws IllegalAccessException if the field can't be accessed.
     */
    private boolean fieldRequiredOrAvailable(JsonField annotation, Field field) throws IllegalAccessException {
        return !annotation.optional() || field.get(this) != null;
    }

    /**
     * Determines if an element is either required by the DTO or available in the JSON object.
     * 
     * @param annotation the annotation representing the element.
     * @param json the JSON object.
     * @return true if the element is either required or available.
     */
    private boolean elementRequiredOrAvailable(JsonField annotation, JSONObject json) {
        return !annotation.optional() || json.has(annotation.name());
    }

    /**
     * Converts a list to a JSON array.
     * 
     * @param list the list.
     * @param type the type of the elements in the list.
     * @return the JSONArray.
     */
    private JSONArray jsonArrayFromList(List<?> list, Class<?> type) {
        JSONArray result = new JSONArray();
        for (Object element : list) {
            if (AbstractDto.class.isAssignableFrom(type)) {
                result.add(((AbstractDto) element).toJson());
            }
            else {
                result.add(element);
            }
        }
        return result;
    }

    /**
     * Converts a JSON array to a list containing elements of the given type.
     * 
     * @param array the JSON array.
     * @param type the element type.
     * @return the list of elements.
     */
    private List<?> listFromJsonArray(JSONArray array, Class<?> type) throws IllegalAccessException {
        List<Object> result = new ArrayList<Object>();
        for (int i = 0; i < array.size(); i++) {
            if (AbstractDto.class.isAssignableFrom(type)) {
                result.add(abstractDtoFromJson(type, array.getJSONObject(i)));
            }
            else {
                result.add(type.cast(array.get(i)));
            }
        }
        return result;
    }

    /**
     * Generates a subclass of AbstractDto from a JSON object.
     * 
     * @param type the specific type to generate.
     * @param json the JSON object representing the field value.
     * @return the object.
     * @throws IllegalAccessException if the JSON object constructor can't be called.
     */
    private AbstractDto abstractDtoFromJson(Class<?> type, JSONObject json) throws IllegalAccessException {
        String typeName = type.getName();
        try {
            Constructor<?> constructor = type.getConstructor(JSONObject.class);
            return (AbstractDto) constructor.newInstance(json);
        }
        catch (InstantiationException e) {
            throw new WorkflowException("unable to call JSON object constructor for " + typeName, e);
        }
        catch (IllegalArgumentException e) {
            throw new WorkflowException("unable to call JSON object constructor for " + typeName, e);
        }
        catch (InvocationTargetException e) {
            throw new WorkflowException("an error occurred in the constructor for " + typeName, e.getCause());
        }
        catch (NoSuchMethodException e) {
            throw new WorkflowException("no JSON object constructor found for " + typeName, e);
        }
    }

    /**
     * Determines whether or not an annotation has a default value.
     * 
     * @param annotation the annotation.
     * @return true if the annotation has a default value.
     */
    private boolean hasDefault(JsonField annotation) {
        return !annotation.defaultValue().equals(JsonField.NULL);
    }
}
