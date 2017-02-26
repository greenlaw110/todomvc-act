package todomvc;

import act.Act;
import act.controller.Controller;
import act.db.morphia.MorphiaAdaptiveRecordWithLongId;
import act.db.morphia.MorphiaDaoWithLongId;
import act.handler.Produces;
import act.route.Router;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.PostLoad;
import org.mongodb.morphia.annotations.PostPersist;
import org.mongodb.morphia.annotations.Transient;
import org.osgl.http.H;
import org.osgl.mvc.annotation.DeleteAction;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PatchAction;
import org.osgl.mvc.annotation.PostAction;
import org.osgl.util.S;

import java.util.Map;

import static act.controller.Controller.Util.notFoundIfNull;

@Entity(value = "todo", noClassnameStored = true)
public class Todo extends MorphiaAdaptiveRecordWithLongId<Todo> {

    @Transient
    public String url;

    @PostLoad
    @PostPersist
    private void updateUrl() {
        url = Act.app().router().fullUrl(S.concat("/todo/", getIdAsStr()));
    }

    @Controller("/todo")
    @Produces(H.MediaType.JSON)
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

        @DeleteAction("{id}")
        public void delete(long id) {
            deleteById(id);
        }
    }

    public static void main(String[] args) throws Exception {
        Act.start("TODO MVC", Todo.class);
    }

}
