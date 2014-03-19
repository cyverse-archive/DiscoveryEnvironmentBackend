package org.iplantc.workflow.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.iplantc.authn.service.UserSessionService;
import org.iplantc.hibernate.util.SessionTask;
import org.iplantc.hibernate.util.SessionTaskWrapper;
import org.iplantc.persistence.dao.user.UserDao;
import org.iplantc.persistence.dto.user.User;
import org.iplantc.workflow.AnalysisNotFoundException;
import org.iplantc.workflow.core.Rating;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.dao.RatingDao;
import org.iplantc.workflow.dao.TransformationActivityDao;
import org.iplantc.workflow.dao.hibernate.HibernateDaoFactory;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Kris Healy <healyk@iplantcollaborative.org>
 */
public class RatingService {
    private SessionFactory sessionFactory;
    private UserSessionService userSessionService;

    /**
     * Extract a user id from the input json.  If there is no user id it pulls
     * one from the user session service.
     */
    private String getUserId(JSONObject input) throws JSONException {
        String userId;

        if(input.has("user_id")) { //$NON-NLS-1$
            userId = input.getString("user_id"); //$NON-NLS-1$
        } else {
            userId = userSessionService.getUser().getUsername();
        }

        return userId;
    }

    /**
     * @see RatingService#getUserRatings(java.lang.String)
     */
    public Map<String, Rating> getUserRatings() {
        return getUserRatings(userSessionService.getUser().getUsername());
    }

    /**
     * Gets all of the user's ratings.  This will populate the resulting
     * JSONObject with transformation analysis ids as keys and user ratings as
     * values.
     *
     * @param username
     *  Username of the user to get the ratings for.
     * @return
     *  Map of user ratings.
     * @throws Exception
     */
    public Map<String, Rating> getUserRatings(String username) {
        Map<String, Rating> result = new HashMap<String, Rating>();
        Session session = sessionFactory.openSession();
        Transaction tx = null;

        try {
            tx = session.beginTransaction();

            DaoFactory daoFactory = new HibernateDaoFactory(session);
            UserDao userDao = daoFactory.getUserDao();
            RatingDao ratingDao = daoFactory.getRatingDao();

            User user = userDao.findByUsername(username);

            if(user == null) {
                throw new RuntimeException("No user found for id " + username); //$NON-NLS-1$
            } else {
                List<Rating> ratings = ratingDao.findByUser(user);

                for (Rating rating : ratings) {
                    result.put(rating.getTransformationActivity().getId(), rating);
                }
            }

            tx.commit();
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            throw new RuntimeException(e);
        } finally {
            session.close();
        }

        return result;
    }

    // commentId = comment id in confluence
    private JSONObject rateAnalysis(Session session, String userId, String analysisId,
            Integer numericRating, Long commentId) throws JSONException {
        DaoFactory daoFactory = new HibernateDaoFactory(session);

        UserDao userDao = daoFactory.getUserDao();
        TransformationActivityDao transformationActivityDao = daoFactory.getTransformationActivityDao();
        RatingDao ratingDao = daoFactory.getRatingDao();

        User user = userDao.findByUsername(userId);
        TransformationActivity transformationActivity = transformationActivityDao.findById(analysisId);

        JSONObject result = new JSONObject();

        if (user == null) {
            throw new RuntimeException("No user found for user id " + userId); //$NON-NLS-1$
        } else if (transformationActivity == null) {
            throw new RuntimeException("No analysis found for analysis id " + analysisId); //$NON-NLS-1$
        } else if (commentId == null) {
            throw new RuntimeException("No comment ID found for analysis id " + analysisId); //$NON-NLS-1$
        } else {
            Rating rating = ratingDao.findByUserAndTransformationActivity(user, transformationActivity);

            if(rating == null) {
                rating = new Rating();

                rating.setUser(user);
                rating.setTransformationActivity(transformationActivity);
                rating.setCommentId(commentId);
            }

            rating.setRaiting(numericRating);

            transformationActivity.getRatings().add(rating);
            transformationActivityDao.save(transformationActivity);

            result.put("avg", transformationActivity.getAverageRating()); //$NON-NLS-1$
        }

        return result;
    }

    public String rateAnalysis(String jsonString) throws Exception {
        JSONObject result = new JSONObject();
        Session session = sessionFactory.openSession();
        Transaction tx = null;

        try {
            tx = session.beginTransaction();

            JSONObject input = new JSONObject(jsonString);
            String userId = getUserId(input);

            result = rateAnalysis(session,
                                  userId,
                                  input.getString("analysis_id"),  //$NON-NLS-1$
                    input.getInt("rating"), //$NON-NLS-1$
                    input.getLong("comment_id")); //$NON-NLS-1$

            tx.commit();
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            throw e;
        } finally {
            session.close();
        }

        return result.toString();
    }

    private void deleteRating(Session session, String userId, String analysisId) throws JSONException {
        DaoFactory daoFactory = new HibernateDaoFactory(session);

        UserDao userDao = daoFactory.getUserDao();
        TransformationActivityDao transformationActivityDao = daoFactory.getTransformationActivityDao();
        RatingDao ratingDao = daoFactory.getRatingDao();

        User user = userDao.findByUsername(userId);
        TransformationActivity transformationActivity = transformationActivityDao.findById(analysisId);

        if (user == null) {
            throw new RuntimeException("No user found for user id " + userId); //$NON-NLS-1$
        } else if (transformationActivity == null) {
            throw new RuntimeException("No analysis found for analysis id " + analysisId); //$NON-NLS-1$
        } else {
            Rating rating = ratingDao.findByUserAndTransformationActivity(user, transformationActivity);

            if(rating != null) {
                transformationActivity.getRatings().remove(rating);
                transformationActivityDao.save(transformationActivity);
            }
        }
    }

    /**
     * Deletes a vote for the current user and returns the new average rating.
     * @param jsonString
     * @return a JSON string containing the new average under the key "avg"
     * @throws Exception
     */
    public String deleteRating(String jsonString) throws Exception {
        Session session = sessionFactory.openSession();
        Transaction tx = null;

        JSONObject input = new JSONObject(jsonString);
        String userId = getUserId(input);
        String analysisId = input.getString("analysis_id"); //$NON-NLS-1$

        try {
            tx = session.beginTransaction();
            deleteRating(session, userId, analysisId);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            throw e;
        } finally {
            session.close();
        }

        // get the new average rating
        JSONObject json = new JSONObject();
        json.put("avg", getAverageRating(analysisId)); //$NON-NLS-1$
        return json.toString();
    }

    private double getAverageRating(final String analysisId) {
        return new SessionTaskWrapper(sessionFactory).performTask(new SessionTask<Double>() {
            @Override
            public Double perform(Session session) {
                TransformationActivityDao dao = new HibernateDaoFactory(session).getTransformationActivityDao();
                TransformationActivity analysis = dao.findById(analysisId);
                if (analysis == null) {
                    throw new AnalysisNotFoundException(analysisId);
                }
                return analysis.getAverageRating();
            }
        });
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public UserSessionService getUserSessionService() {
        return userSessionService;
    }

    public void setUserSessionService(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }
}
