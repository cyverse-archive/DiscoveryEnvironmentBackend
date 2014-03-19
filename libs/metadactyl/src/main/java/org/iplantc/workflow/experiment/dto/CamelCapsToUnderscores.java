package org.iplantc.workflow.experiment.dto;

import net.sf.json.processors.PropertyNameProcessor;

/**
 * A JSON property name processor that converts bean property names from camelCapSeparatedWords to
 * underscore_separated_words.
 * 
 * @author Dennis Roberts
 */
public class CamelCapsToUnderscores implements PropertyNameProcessor {
    
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public String processPropertyName(Class beanClass, String name) {
        String[] words = name.split("(?=\\p{Upper})");
        StringBuffer buffer = new StringBuffer();
        for (String word : words) {
            if (buffer.length() != 0) {
                buffer.append("_");
            }
            buffer.append(word.toLowerCase());
        }
        return buffer.toString();
    }
}
