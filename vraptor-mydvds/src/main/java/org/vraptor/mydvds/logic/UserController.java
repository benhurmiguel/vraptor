package org.vraptor.mydvds.logic;

import java.util.ArrayList;
import java.util.List;

import org.vraptor.mydvds.dao.DaoFactory;
import org.vraptor.mydvds.interceptor.UserInfo;
import org.vraptor.mydvds.model.User;

import br.com.caelum.vraptor.Get;
import br.com.caelum.vraptor.Path;
import br.com.caelum.vraptor.Post;
import br.com.caelum.vraptor.Resource;
import br.com.caelum.vraptor.Result;
import br.com.caelum.vraptor.Validator;
import br.com.caelum.vraptor.validator.Hibernate;
import br.com.caelum.vraptor.validator.Validations;
import br.com.caelum.vraptor.view.Results;

/**
 * The resource <code>UserController</code> handles all user operations,
 * such as adding new users, listing all users, and so on.
 */
@Resource
public class UserController {

	private final DaoFactory factory;
    private final Validator validator;
    private final Result result;
    private final UserInfo userInfo;

	/**
	 * Receives dependencies through the constructor.
	 *
	 * @param result VRaptor result handler.
     * @param validator VRaptor validator.
     * @param factory dao factory.
     * @param userInfo info on the logged user.
	 */
	public UserController(Result result, Validator validator, DaoFactory factory, UserInfo userInfo) {
		this.result = result;
		this.validator = validator;
        this.factory = factory;
        this.userInfo = userInfo;
	}

	/**
	 * Accepts HTTP GET requests.
	 * URL:  /home
	 * View: /WEB-INF/jsp/user/home.jsp
	 *
	 * Shows user's home page containing his Dvd collection.
	 */
	@Path("/home")
	@Get
	public void home() {
	    factory.getUserDao().refresh(userInfo.getUser());
	}

	/**
     * Accepts HTTP GET requests.
     * URL:  /users (only GET requests for this URL)
     * View: /WEB-INF/jsp/user/list.jsp
     *
     * Lists all users.
     */
	@Path("/users")
	@Get
	public void list() {
        List<User> users = new ArrayList<User>();
        // search by hand example
        List<User> usersFromDatabase = this.factory.getUserDao().listAll();
        for (User user : usersFromDatabase) {
            User newUser = new User();
            newUser.setId(user.getId());
            newUser.setName(user.getName());
            users.add(newUser);
        }

        result.include("users", users);
        result.use(Results.page()).forward();
    }

	/**
	 * Accepts HTTP POST requests.
	 * URL:	 /users
	 * View: /WEB-INF/jsp/user/add.jsp
	 *
	 * The "user" parameter will be populated with the request parameters, for example:
	 *
	 * 		/user?user.name="Nico"&user.login="555555"
	 *
	 * automatically populates the name and login parameters on the user object with values Nico and 555555.
	 *
	 * Adds new users to the database.
	 */
	@Path("/users")
	@Post
	public void add(User user) {
	    if (user == null) {
	        return;
	    }

	    result.include("user", user);
	    validateAdd(user);

		this.factory.getUserDao().add(user);
		result.use(Results.logic()).redirectTo(UserController.class).userAdded(user);
	}

	/**
	 * Shows the page with information when a user is successfully added.
	 */
	@Get
	public void userAdded(User user) {
	    result.include("user", user);
	}

	/**
	 * Accepts HTTP GET requests.
	 * URL:  /users/id (for example, /users/42 shows information on the user with id 42)
	 * View: /WEB-INF/jsp/user/view.jsp
	 *
	 * Shows information on the specified user.
	 * @param user
	 */
	@Path("/users/{user.id}")
	@Get
	public void view(User user) {
	    this.factory.getUserDao().refresh(user);
	    result.include("user", user);
	}

    /**
	 * Validation with VRaptor.
	 * Validates user data.
	 */
	private void validateAdd(final User user) {
		validator.checking(new Validations() {{
		    // checks if there is already an user with the specified login
		    boolean loginDoesNotExist = !factory.getUserDao().containsUserWithLogin(user.getLogin());

            that(loginDoesNotExist, "login", "login_already_exists");

		    // calls Hibernate Validator for the user instance
		    and(Hibernate.validate(user));
		}});

		// redirects to the index page if any validation errors occur.
		validator.onErrorUse(Results.page()).of(LoginController.class).index();
	}

}
