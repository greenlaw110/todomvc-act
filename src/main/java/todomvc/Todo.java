package todomvc;

import act.Act;
import act.controller.Controller;
import act.db.morphia.MorphiaAdaptiveRecordWithLongId;
import act.db.morphia.MorphiaDaoWithLongId;
import act.route.Router;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.PostLoad;
import org.mongodb.morphia.annotations.PostPersist;
import org.mongodb.morphia.annotations.Transient;
import org.osgl.http.H;
import org.osgl.mvc.annotation.*;
import org.osgl.util.S;

import java.util.Map;

import static act.controller.Controller.Util.notFoundIfNull;

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

        @GetAction
        public Iterable<Todo> list() {
            return findAll();
        }

        @PatchAction("{id}")
        public Todo update(long id, Map<String, Object> data) {
            Todo todo = findById(id);
            notFoundIfNull(todo);
            todo.mergeValues(data);
            return save(todo);
        }

        @DeleteAction
        public void drop() {
            super.drop();
        }

        @DeleteAction("{id}")
        public void delete(Long id) {
            deleteById(id);
        }
    }

    public static void main(String[] args) throws Exception {
        Act.start("TODO MVC");
    }

}