# CAS Extensions

This project contains some CAS server customizations that are required to
support the way that iPlant uses CAS.  The initial reason for creating this
library was to support the retrieval of reverse group membership from our LDAP
directory, which didn't have the reverse group membership lookup built into
the Person class.

This library can be used in two ways.  One way is to build the JAR file and
place it in the CAS server web application's library directory.  The other
way is to install this library in your local Maven repository and add the
dependency for this library to the pom.xml file for cas-server-webapp.  Once
this library is installed in the CAS server web application, you can use the
library classes in the Spring configuration files for CAS.

## Aggregating Multiple Results for a Single User

This was the primary reason for creating this library.  This can be done using
org.iplantc.persondir.support.AccumulatingPersonAttributeDao in conjunction
with org.iplantc.persondir.support.ldap.LdapMultirecordAttributeDao.  The
Spring configuration, which is usually placed in deployerConfigContext.xml,
would look something like this:

    <bean id="groupAttributeRepository"
          class="org.iplantc.persondir.support.AccumulatingPersonAttributeDao">
        <property name="innerDao">
            <bean class="org.iplantc.persondir.support.ldap.LdapMultirecordAttributeDao">
                <property name="contextSource" ref="contextSource" />
                <property name="requireAllQueryAttributes" value="true" />
                <property name="baseDN" value="ou=Groups,dc=iplantcollaborative,dc=org" />
                <property name="queryAttributeMapping">
                    <map>
                        <entry key="username" value="memberUid" />
                    </map>
                </property>
                <property name="resultAttributeMapping">
                    <map>
                        <entry key="cn" value="entitlement" />
                    </map>
                </property>
            </bean>
        </property>
    </bean>

AccumulatingPersonAttributeDao is responsible for consolidating multiple query
results into a single record.  LdapMultirecordAttributeDao is a version of
org.jasig.services.persondir.support.ldap.LdapPersonAttributeDao that allows
multiple records to be retrieved for a single user.

The configuration that we use at iPlant is a little more complex because we're
combining the results of this query with the information that we're obtaining
from the Person object.  Here's the full attribute repository configuration:

    <bean id="attributeRepository"
          class="org.jasig.services.persondir.support.MergingPersonAttributeDaoImpl">
        <property name="personAttributeDaos">
            <list>
                <ref bean="userAttributeRepository" />
                <ref bean="groupAttributeRepository" />
            </list>
        </property>
    </bean>

    <bean id="userAttributeRepository"
          class="org.jasig.services.persondir.support.ldap.LdapPersonAttributeDao">
        <property name="contextSource" ref="contextSource" />
        <property name="requireAllQueryAttributes" value="true" />
        <property name="baseDN" value="ou=People,dc=iplantcollaborative,dc=org" />
        <property name="queryAttributeMapping">
            <map>
                <entry key="username" value="uid" />
            </map>
        </property>
        <property name="resultAttributeMapping">
            <map>
                <entry key="sn" value="lastName" />
                <entry key="givenName" value="firstName" />
                <entry key="mail" value="email" />
                <entry key="description" value="roles" />
            </map>
        </property>
    </bean>

    <bean id="groupAttributeRepository"
          class="org.iplantc.persondir.support.AccumulatingPersonAttributeDao">
        <property name="innerDao">
            <bean class="org.iplantc.persondir.support.ldap.LdapMultirecordAttributeDao">
                <property name="contextSource" ref="contextSource" />
                <property name="requireAllQueryAttributes" value="true" />
                <property name="baseDN" value="ou=Groups,dc=iplantcollaborative,dc=org" />
                <property name="queryAttributeMapping">
                    <map>
                        <entry key="username" value="memberUid" />
                    </map>
                </property>
                <property name="resultAttributeMapping">
                    <map>
                        <entry key="cn" value="entitlement" />
                    </map>
                </property>
            </bean>
        </property>
    </bean>
