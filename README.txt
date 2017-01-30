CS4262- Distributed Systems
-----------------------------

By editing the port in the pom.xml you can give a new port to the node.
Then execute the following command.

To build and run:
    mvn clean install tomcat7:run

Available APIs
----------------------------------------
NOTE : The port number that you've configured in pom.xml is referred as PORT in following APIs

Connect to the bootstrap
    http://localhost:PORT/connect/{bootstrap_server_ip}/{bootstrap_server_port}/{user_ip}/{username}

List of available movies
    http://localhost:PORT/movies

Search movies
    http://localhost:PORT/search/{movie_name}/{hop_count}

Search movies [with hop count]
    http://localhost:PORT/search/{movie_name}

Get peers
    http://localhost:PORT/peers

Disconnect the node
    http://localhost:PORT/disconnect


