package org.iplantc.hibernate.id;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.type.Type;

public class SharedSequenceStyleGenerator implements Configurable,
		IdentifierGenerator, PersistentIdentifierGenerator {

	private static Map<String, SequenceStyleGenerator> nameGeneratorMap = new HashMap<String, SequenceStyleGenerator>();
	
	private String name;
	
	public void configure(Type type, Properties props, Dialect dialect)
			throws MappingException {
		name = (String) props.get("name");
		if (!nameGeneratorMap.containsKey(name)) {
			SequenceStyleGenerator ssg = new SequenceStyleGenerator();
			ssg.configure(type, props, dialect);
			nameGeneratorMap.put(name, ssg);
		}
	}

	public Serializable generate(SessionImplementor si, Object obj)
			throws HibernateException {
		return nameGeneratorMap.get(name).generate(si, obj);
	}

	public Object generatorKey() {
		return nameGeneratorMap.get(name).generatorKey();
	}

	public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
		return nameGeneratorMap.get(name).sqlCreateStrings(dialect);
	}

	public String[] sqlDropStrings(Dialect dialect) throws HibernateException {
		return nameGeneratorMap.get(name).sqlDropStrings(dialect);
	}

}
