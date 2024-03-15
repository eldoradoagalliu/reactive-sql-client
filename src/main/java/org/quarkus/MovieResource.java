package org.quarkus;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.quarkus.model.Movie;

import java.net.URI;

@Path("movies")
public class MovieResource {

    @Inject
    PgPool client;

    @PostConstruct
    void config() {
        initdb();
    }

    @GET
    public Multi<Movie> getMovies() {
        return Movie.findAll(client);
    }

    @GET
    @Path("/{id}")
    public Uni<Response> getMovie(@PathParam("id") Long id) {
        return Movie.findById(client, id)
                .onItem()
                .transform(movie -> movie != null ? Response.ok(movie) : Response.status(Response.Status.NOT_FOUND))
                .onItem().transform(Response.ResponseBuilder::build);
    }

    @POST
    public Uni<Response> createMovie(Movie movie) {
        return Movie.saveMovie(client, movie.getTitle())
                .onItem().transform(id -> URI.create("/movies/" + id))
                .onItem().transform(uri -> Response.created(uri).build());
    }

    @DELETE
    @Path("{id}")
    public Uni<Response> deleteMovie(@PathParam("id") Long id) {
        return Movie.deleteMovie(client, id)
                .onItem().transform(deleted -> deleted ? Response.Status.NO_CONTENT : Response.Status.NOT_FOUND)
                .onItem().transform(status -> Response.status(status).build());
    }

    private void initdb() {
        client.query("DROP TABLE IF EXSISTS movies").execute()
                .flatMap(m -> client.query("CREATE TABLE movies (id SERIAL PRIMARY KEY, title TEXT NOT NULL)").execute())
                .flatMap(m -> client.query("INSERT INTO movies (title) VALUES ('The Lord of the Rings')").execute())
                .flatMap(m -> client.query("INSERT INTO movies (title) VALUES ('Harry Potter')").execute())
                .await()
                .indefinitely();
    }
}
