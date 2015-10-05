# The NetIDE Network Engine
NetIDE foresees three different environments (sketched in
the Figure below): (i) tools like topology editor, code editors, debuggers in the **Integrated Development Environment** (IDE), (ii) the **Application Repository** which stores simple modules and composed applications, and (iii) the **Network Engine** where the network applications are executed.

The **IDE** includes editors supporting network programming languages and a graphical editor to specify topologies. It reads and writes modules and applications from and to the repository, composing them into new applications, and deploys them on the Network Engine for execution.

The **Network Engine** follows the layered SDN controller approach proposed by the Open Networking Foundation. It comprises a client controller layer that executes the modules network applications are composed of and a server SDN controller layer that drives the underlying infrastructure.

The challenge is to integrate client and server controllers. A first idea is to connect a client’s South-bound Interface (SBI) to a server’s North-bound Interface (NBI). But as these interfaces do not match, adaptation is necessary. This adaptation has to cater for the idiosyncrasies of the controller frameworks and has to be implemented for each single one.
For maximal reuse, we use separate adaptors for the clients’SBI – the Backend – and the server’s NBI – the Shim. This separation necessitates a protocol between them, the NetIDE
Intermediate Protocol.
While such a shim/backend structure connected by an intermediate protocol is feasible, it would still leave substantial adaptation logic in these modules. To overcome this shortcoming, we introduce a further intermediate layer, the Core: it hosts all logic and data structures that are independent of the particular controller frameworks and communicates with both shim and backend using the same NetIDE intermediate protocol. The core makes both shim and backend light-weight and easier to implement for new controllers. Moreover, it provides a convenient place to connect additional run-time tools using a standardized interface. The core introduces some overhead but makes the architecture much more flexible; for production, faster, tightly integrated implementations are easily conceivable.

![Alt text](/NetIDE-architecture.png?raw=true " ")
