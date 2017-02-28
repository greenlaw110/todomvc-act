# A TodoBackend Implementation in [ActFramework](http://actframework.org)

Sample web application implements [todo-backend](http://www.todobackend.com/) using [ActFramework](http://actframework.org) and MongoDB.

Verify the spec compliance at [todobackend testing site](http://www.todobackend.com/specs/index.html?http://todobackend.actframework.org/todo)

View the app on our [demo site](http://todobackend.actframework.org/)

To run locally:
```bash
//Make sure Mongodb is installed locally
$ mvn clean compile exec:exec
```
## How simple a TodoBackend app can be written in X and Y framework

Just check the source code and table below
 
| *Language/Platform* | *Implementation* | *Data Persistent* | *Line of Code* | 
| --------------------------- | --------- | ----------- | -------------- |
| Java/JVM | [ActFramework](https://github.com/greenlaw110/todomvc-act) | MongoDB | 64 |
| Java/JVM | [Spring4 + Boot](https://github.com/jcsantosbr/todo-backend-spring4-java8) | Java Set | 200  |
| Java/JVM | [vertx](https://github.com/VIthulan/todo-vertx) | MongoDB | 241 |
| Java/JVM | [Dropwizard](https://github.com/danielsiwiec/todo-backend-dropwizard) | Java Map | 115 |
| Java/JVM | [Jooby](https://github.com/jooby-project/todo-backend) | Java Map | 231 |
| Java/JVM | [SparkFramework](https://github.com/moredip/todobackend-spark) | PostgreSQL | 348 |
| Kotlin/JVM | [Rapidoid](https://github.com/selvakn/todobackend-kotlin-rapidoid) | Java Map | 81 |
| Closure/JVM | [Closure](https://github.com/akiellor/todo-backend-compojure) | PostgreSql | 142 |
| Scala/JVM | [Scala/Play2.5](https://github.com/jrglee/todo-play-scala-postgres) | PostgreSql | 136 |
| Golang | [Gin](https://github.com/savaki/todo-backend-gin) | Map in memory | 128 |
| Golang | [stdlib](https://github.com/mforman/todo-backend-golang) | In memory data structure | 238 |
| JavaScript/NodeJs | [express](https://github.com/dtao/todo-backend-express) | PostgreSql | 130 |
| Python | [webpy](https://github.com/moredip/todo-backend-py) | Array in memory | 32 |
| Python | [django](https://github.com/mihirk/todo-backend-django) | sqllite | 164 |
| Ruby | [rails](https://github.com/hammerdr/todo-backend-rails) | PostgreSql | 311 |
| PHP | [symfony2](https://github.com/oegnus/symfony2-todobackend) | sqlite | 130 (only count files in `src` dir) |
| Haskell | [Snap](https://github.com/jhedev/todobackend-haskell/blob/master/todobackend-snap) | Sqlite | 98 | 
| C#/.Net | [Asp.Net core](https://github.com/dstockhammer/todo-backend-aspnetcore) | ? (Entity Framework) |887 |
| Swift | [Kitura](https://github.com/IBM-Swift/todolist-mongodb) | MongoDB | 473 |

## How can ActFramework make it so clean?

### The Model

In this implementation we choose MongoDB as our data persistent store. Act provides awesome integration with 
Mongodb through [act-morphia](https://github.com/actframework/act-morphia) plugin, which relies on the official
Morphia object document mapper layer.

A innovative feature Act brings to developer on top of Morphia is called `AdaptiveRecord` which allows the 
backend developer to declare only the fields needs to participate in backend logic. For any fields required
 by frontend and not used in Java app, just ignore them. Thus here is our entity model class for `Todo`:
 
```java
@Entity(value = "todo", noClassnameStored = true)
public class Todo extends MorphiaAdaptiveRecordWithLongId<Todo> {

    // needs to define this property to make it comply with todobackend spec
    // unless https://github.com/TodoBackend/todo-backend-js-spec/issues/6
    // is accepted
    public boolean completed;

    // url is required as per backend test spec. However it is not required
    // to put into the database. So we mark it as Transient property
    @Transient
    public String url;

    // We will generate the derived property `url` after
    // saving the model and loading the model
    @PostLoad
    @PostPersist
    private void updateUrl() {
        url = Act.app().router().fullUrl(S.concat("/todo/", getIdAsStr()));
    }
}
```

We don't need to declare all fields presented on front end, e.g. `title`, `order` and even `completed`
which is declared in the source code because of [this issue](https://github.com/TodoBackend/todo-backend-js-spec/issues/6) 
in the test spec
 
Note the `url` is not part of the data to be persist into our data store, instead it is a derived 
property that concatenate the `GET` action URL path and the entity's id. We relies on Morphia's
`PostLoad` and `PostPersist` lifecycle callback method to init the property.

### The Service

It is very unusual to get service class nested into the entity model class like what we did in this
implementation:

```java
@Entity(value = "todo", noClassnameStored = true)
public class Todo extends MorphiaAdaptiveRecordWithLongId<Todo> {

    // needs to define this property to make it comply with todobackend spec
    // unless https://github.com/TodoBackend/todo-backend-js-spec/issues/6
    // is accepted
    public boolean completed;

    ....

    @Controller("/todo")
    @ResponseContentType(H.MediaType.JSON)
    public static class Service extends MorphiaDaoWithLongId<Todo> {

        @PostAction
        public Todo create(Todo todo, Router router) {
            return save(todo);
        }

        @GetAction("{id}")
        public Todo show(long id) {
            return findById(id);
        }
        ...
    }
}
```

However in the TODO application we think it is not a bad choice because our TODO service only operates on one 
resource type, i.e. the TODO item. Actually it is encouraged because

1. The operation (service endpoint logic) and the data (entity model) sit together means high cohesion in the app. 
The design mimic the Object Oriented Programming that encapsulate the data and the operation on the data into a
single module.
2. It improves readability, as we don't need to switch between classes or even packages to find the data model
when I am reading the service operating on the data.

And of course we can choose this tight and clean pattern to organize our TODO showcase because Actframework provides
the great flexibility to allow it put the action handler into any place given the action annotation is presented.

BTW, we can even take out the `@Produces(H.MediaType.JSON)` line if the test spec accepted and fixed 
[this issue](https://github.com/TodoBackend/todo-backend-js-spec/issues/5)

### Where is the code support CORS

It is required by TodoBackend that showcase application [must enable CORS](http://www.todobackend.com/contribute.html).
Thus we see the code like the following:

From [Java 8 with Spring 4 Boot](https://github.com/jcsantosbr/todo-backend-spring4-java8/blob/master/src/main/java/com/jcs/todomvc/SimpleCORSFilter.java)

```java
public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
    HttpServletResponse response = (HttpServletResponse) res;
    response.setHeader("Access-Control-Allow-Origin", "*");
    response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PATCH");
    response.setHeader("Access-Control-Max-Age", "3600");
    response.setHeader("Access-Control-Allow-Headers", "x-requested-with, origin, content-type, accept");
    chain.doFilter(req, res);
}
```

Or from [Vert.x and PostgreSQL](https://github.com/tirnak/vert.x-todo-backend-postgresql/blob/master/src/main/java/org/kirill/todo/ToDoApplication.java)

```java
// CORS enabling
router.route().handler(CorsHandler.create("*")
        .allowedMethod(HttpMethod.GET)
        .allowedMethod(HttpMethod.POST)
        .allowedMethod(HttpMethod.OPTIONS)
        .allowedMethod(HttpMethod.DELETE)
        .allowedMethod(HttpMethod.PATCH)
        .allowedHeader("X-PINGARUNER")
        .allowedHeader("Content-Type"));
```

Or from [Java with Dropwizard](https://github.com/danielsiwiec/todo-backend-dropwizard/blob/master/src/main/java/service/TodoApplication.java)

```java
private void addCorsHeader(Environment environment) {
    FilterRegistration.Dynamic filter = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
    filter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
    filter.setInitParameter("allowedOrigins", "*");
    filter.setInitParameter("allowedMethods", "GET,PUT,POST,DELETE,OPTIONS,HEAD,PATCH");
}
```

However we don't see any of these CORS relevant code in the ActFramework implementation. The only thing we've done
is add `cors=true` in the 
[config file](https://github.com/greenlaw110/todomvc-act/blob/master/src/main/resources/conf/common/app.properties)
This is another cool stuff about Act, it integrates utilities supporting common features in a web app including CORS, 
CSRF etc.

In summary, ActFramework provides a flexible and powerful infrastructure that support creating RESTful service
in a much simpler way. With ActFramework the developer just need to focus on business logic, not plumbing.

