package com.uom.cse.distsearch;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;


@Path("/service")
public class Service {
    /**
     * Logger to log the events.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Service.class);

    @Context
    private ServletContext context;

    @Context
    private HttpServletRequest httpRequest;

    private App app = App.getInstance();

    @Path("/movies")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listMovies() {
        LOGGER.debug("Request to list the selected movies");
        MovieList movieList = MovieList.getInstance(context.getRealPath("/WEB-INF/movies.txt"));
        return Response.status(Response.Status.OK).entity(movieList.getSelectedMovies()).build();
    }

    @Path("/peers")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listPeers() {
        LOGGER.debug("Request to list the connected peers");
        List<Node> lst = new ArrayList<>();
        lst.add(app.predecessor);
        lst.add(app.successor);
        LOGGER.debug("PEERS {}", lst.toString());
        return Response.status(Response.Status.OK).entity(lst).build();
    }

    @Path("/connect/{serverip}/{serverport}/{userip}/{username}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response connect(@NotNull @PathParam("serverip") String serverIP, @NotNull @PathParam("serverport") int serverPort, @NotNull @PathParam("userip") String userip, @NotNull @PathParam("username") String username) {
        LOGGER.debug("Request to connect to the bootstrap server");
        // Connect to the Bootstrap
        Response.Status status = Response.Status.OK;
        if (!app.connect(serverIP, serverPort, userip, httpRequest.getLocalPort(), username)) {
            status = Response.Status.INTERNAL_SERVER_ERROR;
        }
        // Disconnect from the Bootstrap
        return Response.status(status).build();
    }

    @Path("/disconnect")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response disconnect() {
        LOGGER.debug("Request to disconnect from the bootstrap server");
        Response.Status status = Response.Status.OK;
        if (!app.disconnect()) {
            status = Response.Status.INTERNAL_SERVER_ERROR;
        }
        // Disconnect from the Bootstrap
        return Response.status(status).build();
    }

    @Path("/pjoin")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response pJoin(@NotNull Node node) {
        LOGGER.debug("Request to pJoin from {}", node);
        //app.pJoin(node);


//        ip = tokenizer.nextToken();
//        port = Integer.parseInt(tokenizer.nextToken());
//        if(successor!=null){
//            successorConnect(new Neighbour(ip,port,""),successor);
//        }
//        successor = new Neighbour(ip,port,"");
//        String reply = "0014 "+Command.PredecessorJOINOK+" 0";
//        send(new Communicator(ip,port,reply));

        if(app.successor!=null){
            app.post(node.url() + "sjoin", app.successor);
        }
        app.successor= node;
        return Response.status(Response.Status.OK).entity(Command.JOINOK).build();
    }

    @Path("/sjoin")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response sJoin(@NotNull Node node) {
        LOGGER.debug("Request to pJoin from {}", node);
        app.pJoin(node);
//        ip = tokenizer.nextToken();
//        port = Integer.parseInt(tokenizer.nextToken());
//        predecessorConnect(new Neighbour(ip,port,""));
        return Response.status(Response.Status.OK).entity(Command.JOINOK).build();
    }

    @Path("/leave")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response leave(@NotNull Node node) {
        LOGGER.debug("Request to leave from {}", node);
        app.leave(node);
        return Response.status(Response.Status.OK).entity(Command.LEAVEOK).build();
    }

    @Path("/searchuser")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response searchuser(@NotNull @QueryParam("query") String query) {
        LOGGER.debug("Request to search {}", query);
        MovieList movieList = MovieList.getInstance(context.getRealPath("/WEB-INF/movies.txt"));
        app.startSearch(movieList, query);
        return Response.status(Response.Status.OK).entity(Command.SEROK).build();
    }

    @Path("/search")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response search(@NotNull @Encoded Query query) {
        //LOGGER.debug("Request to search {} from {}", query.getQueryInfo().getQuery(), query.getSender());
        MovieList movieList = MovieList.getInstance(context.getRealPath("/WEB-INF/movies.txt"));
        app.search(movieList, query);
        return Response.status(Response.Status.OK).entity(Command.SEROK).build();
    }

    @Path("/results")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response results(@NotNull Result result) {
        int moviesCount = result.getMovies().size();

        String output = String.format("Number of movies: %d\r\nMovies: %s\r\nHops: %d\r\nTime: %s millis\r\nOwner %s:%d",
                moviesCount, result.getMovies().toString(), result.getHops(), (System.currentTimeMillis() - result.getTimestamp()), result.getOwner().getIp(), result.getOwner().getPort());

        LOGGER.info(output);
        return Response.status(Response.Status.OK).entity("OK").build();
    }
}
