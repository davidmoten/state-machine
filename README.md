state-machine
==============
<a href="https://travis-ci.org/davidmoten/state-machine"><img src="https://travis-ci.org/davidmoten/state-machine.svg"/></a><br/>
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.davidmoten/state-machine/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/com.github.davidmoten/state-machine)<br/>
<!--[![Dependency Status](https://gemnasium.com/com.github.davidmoten/state-machine.svg)](https://gemnasium.com/com.github.davidmoten/state-machine)-->

Generates java classes to handle state transitions based on a state machine defined with type safety in java. Supports 
immutability (though is not opinionated in the sense that if you want to mutate your objects you can).

* Concise API using method chaining
* leverages the simplicity of the Exectuable UML approach
to state diagrams (one Event type for each State)
* Maven plugin
* Not coupled to a storage mechanism (both a feature and a non-feature!)
* optional reactive API using [RxJava](https://github.com/ReactiveX/RxJava) (very useful for asynchronous coordination and for extensions like storage if desired)

Status: *pre-alpha*

Example State Diagram
------------------------
Consider a microwave. If you were going to write the control system for a microwave you'd find it's a natural candidate for a state machine (actually all programming is working with state machines but we are going to work with one quite explicitly). We are going to work with a very simple microwave design (one you'd be crazy to buy in the shops!) just to demonstrate state diagrams.

<img src="src/docs/microwave-state-diagram.png?raw=true" />

The definition looks like this:

```java
//define states
StateMachine<Microwave> m = 
    StateMachine.create(Microwave.class);
State<DoorClosed> readyToCook = 
    m.createState("Ready to Cook", DoorClosed.class);
State<DoorOpened> doorOpen = 
    m.createState("Door Open", DoorOpened.class);
State<ButtonPressed> cooking = 
    m.createState("Cooking", ButtonPressed.class);
State<DoorOpened> cookingInterruped = i
    m.createState("Cooking Interrupted", DoorOpened.class);
State<TimerTimesOut> cookingComplete = i
    m.createState("Cooking Complete", TimerTimesOut.class);

//define transitions
readyToCook
  .to(cooking)
  .to(cookingInterruped)
  .to(readyToCook
       .from(doorOpen
              .from(readyToCook)
              .from(cookingComplete
                     .from(cooking))));
```
As you can see the definition is pretty concise. This is largely because of the advisable constraint that any one State can only be arrived at via one Event type. Note that you can still use inheritance of the Event if you wish so that for say a `Position` event you could pass the state machine a `PositionWithSpeed` event or a `SimplePosition event` as long as they both inherit from `Position`.

`a.to(b)` records that there is a transition from `a -> b` and returns `b`.

`a.from(b)` records there is a transition from `b -> a` and returns `a`.  

Generating code
----------------

From the above state machine definition for a microwave we generate these classes:

* `MicrowaveStateMachine` - throw events at this to see transitions and to collect signals to self and others emitted during the new state's *entry procedure* 
* `MicrowaveBehaviour` - interface to capture entry procedures
* `MicrowaveBehaviourBase` - a do-nothing implentation of `MicrowaveBehaviour` that is convenient for inheriting

The cleanest implementation of this generation is using maven artifacts.

Create a maven artifact like [state-machine-test-definition](state-machine-test-definition) and customize `StateMachines.java`.

Then in the project where you want to have the generated classes, set it up like [state-machine-test](state-machine-test). That means adding dependencies on your definition artifact to the pom and also snippets using *state-machine-maven-plugin* and *build-helper-maven-plugin* to your pom.xml. Then you will have generated state machine classes to use in your program.

Once you have generated these classes you can use the *Rx* helpers or you can do your own thing:

```java
MicrowaveBehaviour behaviour = new MicrowaveBehaviourBase();
MicrowaveStateMachine m = 
    MicrowaveStateMachine
      .create(microwave, 
              "1", 
              behaviour,
              MicrowaveStateMachine.State.READY_TO_COOK);
m = m.signal(new ButtonPressed())
     .signal(new DoorOpened());
```

Identifiers
----------------
Every object controlled by a state machine must have a unique id. When an immutable object is transformed by a state transition then the transformed object must have the same id as the original object.

Generating a diagram
-------------------------
It's a great idea to generate a diagram from what you have coded to ensure it is what you expect. In the final product you might choose to unit test all transitions but while you are exploring the requirements and your design it's really useful to visualize the state machines your are creating. 

After numerous experiments over the years I've settled on generating a [GraphML](http://graphml.graphdrawing.org/) file and using the excellent free 
tool [yEd](https://www.yworks.com/products/yed) to automate the layout. The state-machine maven plugin generates code but also generates `.graphml` files (with some *yEd* extensions) for each state machine that can be opened in *yEd*. Select **Layout - Orthogonal - UML Style** (Alt-Shift-U) and a dialog will appear. The setting `Grid` in the dialog affects the internode spacing so play with that as you wish. The results are excellent!

<img src="state-machine-test/src/docs/com.github.davidmoten.fsm.example.microwave.Microwave.png?raw=true" />

The state-machine maven plugin also generates a more detailed state diagram that includes documentation of each state in the diagram nodes. This is how html documentation is associated with each state:

```java
State<DoorClosed> readyToCook = 
    m.createState("Ready to Cook", DoorClosed.class)
     .documentation("<pre>entry/\nturn light off;</pre>");
```

In the example below the documentation is a pseudo-code description of the *entry procedures* for each state (discussed in [Behaviour](https://github.com/davidmoten/state-machine#behaviour) section below):

<img src="state-machine-test/src/docs/com.github.davidmoten.fsm.example.microwave.Microwave-with-docs.png?raw=true" />

On my linux machine I have a command line alias for *yEd*: `alias yed='java -jar /opt/yed/current/yed.jar'` so that I can automate the regeneration of the diagram from the command line like this:

```bash
mvn clean install && yed state-machine/state-machine-test/target/state-machine-docs/com.github.davidmoten.fsm.example.microwave.Microwave.graphml
```

I hit `Alt-Shift-U` to do the layout and then export the diagram as I please (**File - Export**).

Behaviour
---------------
When a transition occurs in a state machine from state A to state B, the transition is not considered complete till the *entry procedure* for B has been run. Behaviour is specified according to a generated interface and is given to an instance of `MicrowaveStateMachine` at creation. For instance to specify that when a Microwave enters the Cooking state that it will time out and stop cooking after 30 seconds (transition to state Cooking Complete) we would implement the behaviour for a Microwave like this:

```java
MicrowaveBehaviour behaviour = new MicrowaveBehaviourBase() {
    @Override
    public Microwave onEntry_Cooking(Signaller signaller, Microwave microwave,
            Object id, ButtonPressed event) {
        signaller.signalToSelf(new TimerTimesOut(), 30, TimeUnit.SECONDS);
        return microwave;
    }
};
```

The signaller has these methods:

```java
public interface Signaller {

    void signalToSelf(Event<?> event);
    
    void signalToSelf(Event<?> event, long delay, TimeUnit unit);

	<T> void signal(Class<T> cls, Object id, Event<?> event);
	
	<T> void signal(Class<T> cls, Object id, Event<?> event, long delay, TimeUnit unit);
	
    void cancelSignal(Class<?> fromClass, Object fromId, Class<?> toClass, Object toId);
}
```

When the *onEntry* procedure is run all signals to self and to others are collected. Upon completion the signals to self are run first in order they were called in the procedure and then the signals to others are run.

At any one time there should only be one outstanding scheduled (non-immediate) signal between object 1 and object 2. This is clearly a pre-requisite for `cancelSignal` to make sense in its present form and is a nice simplification generally.

Signals to self are actioned synchronously but signals to others may be actioned asynchronously.

Rx processing of signals
-------------------------
The runtime artifact has optional support for a reactive implementation of the processing of signals using [RxJava](https://github.com/ReactiveX/RxJava).

Add this artifact to your runtime pom.xml:

```xml
<dependency>
    <groupId>io.reactivex</groupId>
    <artifactId>rxjava</artifactId>
    <version>1.x.x</version>
</dependency>
```

[ProcessorTest.java](state-machine-test/src/test/java/com/github/davidmoten/fsm/rx/ProcessorTest.java) demonstrates usage of an Rx processor for a state machine (or set of state machines).

Persistence
---------------
For a system to recover properly from failure all signals and state changes should be persisted to media that survives process restarts.

Having had scaling problems with a system that persisted every signal and state change to a database was one of the motivations to develop this project. 

A popular strategy for dealing with this issue is to use [Event Sourcing](http://martinfowler.com/eaaDev/EventSourcing.html) and [CQRS](http://martinfowler.com/bliki/CQRS.html). 

Given a stream of events to the state machine, 
* persist only those events that bring about a transition in the state machine
* optionally supplement persisted events with a sample of the incoming stream (so that if state machine rules change we can replay). That sample may of course be the full resolution stream.
* periodically persist the entities in the case when replay using full history of events could be very time-consuming

