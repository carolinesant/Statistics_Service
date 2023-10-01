# Statistics service
An implementation of a simple client-server architecture with a load balancer (proxy server). JAVA-RMI programming model and remote method invocation is used.

The systems works with 5 servers having access to a statistics database. The server parses the dataset and provides the clients with
statistics information about different countries. 

The Application functionality is provided by a remote object residing at the server side. Client objects interact
with the server through remote method invocations. The client can invoke the methods defined
in the server’s remote interface specification. The system consists of a load balancing server, and 5 other server.
The load balancing server acts as a proxy and is in charge of distributing users’ requests
to other 5 servers. Clients make a remote method call to the proxy server and it replies with the
address and port number of one of the 5 servers to clients. Clients then invoke a remote call to
the server that got their address and port and send their request.
