Jingpeng Wu
Networks Project

Instructions:
All files are included in the same directory. Run the main for an example.

Design:
The proxy and server have a lot of similarities so they share a superclass. The client is a separate class and shares many components with the proxy server.
There are TCP and UDP Connector classes which basically contain the socket and low level state information.
There is no tree maintained; each node only keeps the local information needed which achieves the same result.

TCP / UDP shares the same socket. On bind, a ServerSocket is never unbinded, instead it is recycled so there are always
5 active listener (TCP / UDP) threads waiting for children at any time. There is one parent thread facing upwards that is also permanent.
The appropriate concurrent data structures are used.

Add/Prune
Requests are always forwarded to the server via TCP + specially formatted control messages; when an approval or rejection is forwarded down, 
then state keeping action is taken by the thread. UDP messages are cached and sent to neighbors with the same multicast and not echoed by the server.

