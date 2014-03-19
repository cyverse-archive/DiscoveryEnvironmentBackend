package org.iplantc.workflow.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.jdbc.Work;
import org.hsqldb.cmdline.SqlFile;
import org.hsqldb.cmdline.SqlToolError;
import org.iplantc.hibernate.util.HibernateUtil;

/**
 * Used to initialize a database for unit testing. During unit testing, we're using an in-memory HyperSQL database
 * that is completely refreshed before each test.
 * 
 * @author Dennis Roberts
 */
public class DatabaseInitializer {

    private static final String CONFIG = "hibernate-test.cfg.xml";
    private static final String[] SQL_INIT_FILES = {};

    /**
     * Initializes the database for testing.
     */
    public void initializeDatabase() throws Exception {
        Class.forName("org.hsqldb.jdbc.JDBCDriver");
        Connection connection = DriverManager.getConnection("jdbc:hsqldb:mem:workflow");
        assert (connection != null);
        Configuration configuration = new Configuration().configure(CONFIG);
        SessionFactory sessionFactory = configuration.buildSessionFactory();
        HibernateUtil.setSessionFactoryForTesting(sessionFactory);
        createTables();
    }

    /**
     * Deletes the database after testing is complete.
     */
    public void deleteDatabase() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            executeSqlStatement(session, "SHUTDOWN");
            tx.commit();
        }
        catch (RuntimeException e) {
            if (tx != null) {
                tx.rollback();
            }
            throw e;
        }
        finally {
            if (session != null) {
                session.close();
            }
        }
    }

    /**
     * Creates the database tables.
     */
    private void createTables() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            for (String sqlFile : SQL_INIT_FILES) {
                executeSqlFile(session, sqlFile);
            }
            tx.commit();
        }
        catch (RuntimeException e) {
            if (tx != null) {
                tx.rollback();
            }
            throw e;
        }
        finally {
            session.close();
        }
    }

    /**
     * Executes an SQL statement against the database.
     * 
     * @param session the Hibernate session.
     * @param sql the SQL statement to execute.
     */
    private void executeSqlStatement(Session session, final String sql) {
        session.doWork(new Work() {
            @Override
            public void execute(Connection connection) throws SQLException {
                connection.createStatement().execute(sql);
            }
        });
    }

    /**
     * Executes all of the statements in the given SQL file.
     * 
     * @param session the Hibernate session.
     * @param filename the path to the SQL file, relative to some location in the classpath.
     */
    private void executeSqlFile(Session session, final String filename) {
        session.doWork(new Work() {
            @Override
            public void execute(Connection connection) throws SQLException {
                SqlFile sqlFile;
                try {
                    URL resource = ClassLoader.getSystemResource(filename);
                    if (resource == null) {
                        throw new RuntimeException("unable to find " + filename);
                    }
                    sqlFile = new SqlFile(new File(resource.toURI()));
                    sqlFile.setConnection(connection);
                    sqlFile.execute();
                }
                catch (IOException e) {
                    throw new SQLException(e);
                }
                catch (URISyntaxException e) {
                    throw new SQLException(e);
                }
                catch (SqlToolError e) {
                    throw new SQLException(e);
                }
            }
        });
    }
}
