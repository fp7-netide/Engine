NetIDE aims to deliver a single development environment to support the whole development lifecycle of Software-Defined Network programs in a vendor- and controller-independent fashion. Nowadays, while most of the programmable network apparatus vendors support OpenFlow or other south-bound protocols, a number of fragmented control plane solutions exist for Software-Defined Networks. Thus, network applications developers need to re-code their solutions every time they encounter a network infrastructure based on a different controller. NetIDE is approaching the problem through an architectural solution that will allow different high-level representations to be used to program the network and different controllers to execute the network programs. Our core work is the definition of a common language able to cover different network programming styles: the NetIDE IRF (Intermediate Representation Format). The IRF allows us to explore new techniques to perform cross-controller debugging and profiling of network programs; heterogeneous network programming; network programming with simulators in the loop.

NetIDE is an opensource project licensed under the Eclipse Public License.

#Engine

This repository contains the so-called **Network App Engine** of the NetIDE project. The main objective is to deliver a comprehensive toolkit for Network App developers that covers NetIDE methodology. This main objective
can be breakdown into the following sub-objectives:
- Define a methodology for SDN developers covering the whole SDN development lifecycle, with special focus
on design, execution, and testing.
- Develop Transformation APIs to support the translation from specification languages into NetIDE IRF.
- Provide configuration and deployment support for Network Apps in the NetIDE Network App Engine.
- Integrate in NetIDE Developer Toolkit various analysis tools like debugger, profiler or analytical or
simulation-based performance prediction methods.

One important part of the Network App Engine is the NetIDE API Interceptor, which consists of two main components: Controller platform backends and shim clients.

At the same time, it is related to the **IDE** repository (which provides editor support for the engine) and the **Usecases** repository (which first implements three use cases with different SDN platforms, which will be later integrated into the engine).

![Alt text](/NetIDE-architecture.png?raw=true " ")

##
