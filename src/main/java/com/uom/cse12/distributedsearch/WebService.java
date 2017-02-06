package com.uom.cse12.distributedsearch;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;


@Path("/")
public class WebService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebService.class);

    @Context
    private ServletContext context;

    @Context
    private HttpServletRequest httpRequest;

    private App app = App.getInstance();


    @Path("/connect/{serverip}/{serverport}/{userip}/{username}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response connect(@NotNull @PathParam("serverip") String serverIP, @NotNull @PathParam("serverport") int serverPort, @NotNull @PathParam("userip") String userip, @NotNull @PathParam("username") String username) {

        Response.Status status = Response.Status.OK;
        if (!app.connect(serverIP, serverPort, userip, httpRequest.getLocalPort(), username)) {
            status = Response.Status.INTERNAL_SERVER_ERROR;
        }

        return Response.status(status).entity(Command.REGOK).build();
    }

    @Path("/search/{queryText}/{hopLimit}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response search(@NotNull @PathParam("queryText") String queryText, @NotNull @PathParam("hopLimit") int hopLimit) {
        LOGGER.debug("Request to search {}", queryText);
        MovieList movieList = MovieList.getInstance(context.getRealPath("/WEB-INF/movies.txt"));
        app.initiateSearch(movieList, queryText,hopLimit);
        return Response.status(Response.Status.OK).entity(Command.SEROK).build();
    }

    @Path("/search/{queryText}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response searchWithoutHops(@NotNull @PathParam("queryText") String queryText) {
        LOGGER.debug("Request to search {}", queryText);
        MovieList movieList = MovieList.getInstance(context.getRealPath("/WEB-INF/movies.txt"));
        app.initiateSearch(movieList, queryText,9999999);
        return Response.status(Response.Status.OK).entity(Command.SEROK).build();
    }

    @Path("/stat")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getStat() {
        return Response.status(Response.Status.OK).entity(app.getStats().toString()).build();
    }

    @Path("/clear-stat")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response clearStat() {
        app.clearStats();
        return Response.status(Response.Status.OK).entity("OK").build();
    }

    @Path("/query")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response query(@NotNull int noOfNodes) {
        MovieList movieList = MovieList.getInstance(context.getRealPath("/WEB-INF/movies.txt"));
        app.remoteQery(context.getRealPath("/WEB-INF/movies.txt"),movieList,noOfNodes);
        return Response.status(Response.Status.OK).entity("OK").build();
    }

    @Path("/get-stats")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response getStat(@NotNull int noOfNodes) {
        return Response.status(Response.Status.OK).entity(app.getStats().getEncodedStat()).build();
    }

    @Path("/disconnect")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response disconnect() {
        LOGGER.debug("Request to disconnect from the bootstrap server");
        Response.Status status = Response.Status.OK;
        if (!app.disconnect()) {
            status = Response.Status.INTERNAL_SERVER_ERROR;
        }
        return Response.status(status).entity(Command.UNROK).build();
    }

    @Path("/movies")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listMovies() {
        MovieList movieList = MovieList.getInstance(context.getRealPath("/WEB-INF/movies.txt"));
        return Response.status(Response.Status.OK).entity(movieList.getSelectedMovies()).build();
    }

    @Path("/peers")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listPeers() {
        List<Node> lst = app.getPeers();
        return Response.status(Response.Status.OK).entity(lst).build();
    }

    @Path("/join")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response join(@NotNull Node node) {
        app.join(node);
        return Response.status(Response.Status.OK).entity(Command.JOINOK).build();
    }

    @Path("/leave")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response leave(@NotNull Node node) {
        app.leave(node);
        return Response.status(Response.Status.OK).entity(Command.LEAVEOK).build();
    }

    @Path("/search")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response search(@NotNull @Encoded Query query) {
        app.receivedMessages++;
        MovieList movieList = MovieList.getInstance(context.getRealPath("/WEB-INF/movies.txt"));
        app.search(movieList, query);
        return Response.status(Response.Status.OK).entity(Command.SEROK).build();
    }

    @Path("/results")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response results(@NotNull Result result) {
        app.receivedMessages++;
        app.onResultReceived(result);
        return Response.status(Response.Status.OK).entity("OK").build();
    }
}