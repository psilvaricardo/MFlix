package mflix.api.daos;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import mflix.api.models.Session;
import mflix.api.models.User;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Optional;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

@Configuration
public class UserDao extends AbstractMFlixDao {

    private final MongoCollection<User> usersCollection;
    
    private final MongoCollection<Session> sessionsCollection;

    private final Logger log;

    @Autowired
    public UserDao(
            MongoClient mongoClient, @Value("${spring.mongodb.database}") String databaseName) {
        super(mongoClient, databaseName);
        CodecRegistry pojoCodecRegistry =
                fromRegistries(
                        MongoClientSettings.getDefaultCodecRegistry(),
                        fromProviders(PojoCodecProvider.builder().automatic(true).build()));

        usersCollection = db.getCollection("users", User.class).withCodecRegistry(pojoCodecRegistry);
        log = LoggerFactory.getLogger(this.getClass());
        //TODO> Ticket: User Management - implement the necessary changes so that the sessions
        // collection returns a Session objects instead of Document objects.
        sessionsCollection = db.getCollection("sessions", Session.class).withCodecRegistry(pojoCodecRegistry);
    }

    /**
     * Inserts the `user` object in the `users` collection.
     *
     * @param user - User object to be added
     * @return True if successful, throw IncorrectDaoOperation otherwise
     */
    public boolean addUser(User user) 
    {
        try
        {
            // "update" or "insert" only if it does not exist.
            usersCollection.withWriteConcern(WriteConcern.MAJORITY).insertOne(user);
        }
        catch (MongoException me) 
        {
            //TODO > Ticket: Handling Errors - make sure to only add new users
            // and not users that already exist.
            log.error("An error ocurred while trying to insert a User.");
            log.error( me.getLocalizedMessage() );
            
            if ( ErrorCategory.fromErrorCode( me.getCode() ) 
                    == ErrorCategory.DUPLICATE_KEY) {
                throw new IncorrectDaoOperation("The User is already in the database.");
            }
            return false;
        }
        catch ( Exception e )
        {
            log.error("An error ocurred while trying to insert a User.");
            log.error( e.getLocalizedMessage() );
            return false;
        }
        
        return true;
    }

    /**
     * Creates session using userId and jwt token.
     *
     * @param userId - user string identifier
     * @param jwt    - jwt string token
     * @return true if successful
     */
    public boolean createUserSession(String userId, String jwt) {
        // Ticket: User Management - implement the method that allows 
        // session information to be stored in it's designated collection.
        Session session = new Session();
        session.setUserId(userId);
        session.setJwt(jwt);

        try
        {
            if (Optional.ofNullable(sessionsCollection.
                    find( eq("user_id", userId) ).first()).isPresent() ) {
                sessionsCollection.updateOne(eq("user_id", userId), set("jwt", jwt));
            }
            else
            {
                sessionsCollection.insertOne(session);
            }
        }
        catch (MongoException e) {
            log.error("An error ocurred while trying to insert/update a Session.");
            return false;
        }
        
        return true;
    }

    /**
     * Returns the User object matching the an email string value.
     *
     * @param email - email string to be matched.
     * @return User object or null.
     */
    public User getUser(String email) {
        User user;
        user = usersCollection.find(eq("email", email)).first();
        return user;
    }

    /**
     * Given the userId, returns a Session object.
     *
     * @param userId - user string identifier.
     * @return Session object or null.
     */
    public Session getUserSession(String userId) {
        //TODO> Ticket: User Management - implement the method that returns 
        // Sessions for a given userId
        return sessionsCollection.find(eq("user_id", userId)).first();
    }

    public boolean deleteUserSessions(String userId) {
        sessionsCollection.deleteMany(eq("user_id", userId));
        return true;
    }

    /**
     * Removes the user document that match the provided email.
     *
     * @param email - of the user to be deleted.
     * @return true if user successfully removed
     */
    public boolean deleteUser(String email) {
        // remove user sessions
        // Ticket: User Management - implement the delete user method
        // Ticket: Handling Errors - make this method more robust by
        // handling potential exceptions.
        
        sessionsCollection.deleteMany(eq("user_id", email));
        usersCollection.deleteMany(eq("email", email));
        
        try 
        {
            sessionsCollection.deleteMany(eq("user_id", email));
            usersCollection.deleteMany(eq("email", email));
        }
        catch (MongoException e) {
            log.error("An error ocurred while trying to delete a User.");
            return false;
        }
    
        return true;
    }

    /**
     * Updates the preferences of an user identified by `email` parameter.
     *
     * @param email           - user to be updated email
     * @param userPreferences - set of preferences that should be stored and replace the existing
     *                        ones. Cannot be set to null value
     * @return User object that just been updated.
     */
    public boolean updateUserPreferences(String email, Map<String, ?> userPreferences) {
        // Ticket: User Preferences - implement the method that allows for user preferences to
        // be updated.
        // Ticket: Handling Errors - make this method more robust by
        // handling potential exceptions when updating an entry.
        
        try
        {
            usersCollection.updateOne( 
                eq("email", email), 
                set("preferences", 
                Optional.ofNullable(userPreferences).orElseThrow( () -> 
                new IncorrectDaoOperation("user preferences cannot be null") ) ) );
        }
        catch (MongoException e) 
        {
            log.error("An error ocurred while trying to update User preferences.");
            return false;
        }
        
    return true;
    
    }
}
