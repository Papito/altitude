package altitude

import altitude.models.{Repository, User}

/**
 * The Context is the object that is implicitly passed from the service layer
 * (or test layer) to other services and ultimately the data access layer,
 * to keep track of what repository and which user we are operating on.
 */
class Context(val repo: Repository, val user: User)
