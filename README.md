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

Status: *pre-alpha*

Example State Diagram
------------------------
Consider a microwave. If you were going to write the control system for a microwave you'd find it's a natural candidate for a state machine (actually all programming is working with state machines but we are going to work with one quite explicitly).

<img src="src/docs/microwave-state-diagram.png?raw=true" />

The definition looks like this:

```java
StateMachine<Microwave> m = StateMachine.create(Microwave.class);
State<DoorClosed> readyToCook = m.state("Ready to Cook", DoorClosed.class);
State<DoorOpened> doorOpen = m.state("Door Open", DoorOpened.class);
State<ButtonPressed> cooking = m.state("Cooking", ButtonPressed.class);
State<DoorOpened> cookingInterruped = m.state("Cooking Interrupted", DoorOpened.class);
State<TimerTimesOut> cookingComplete = m.state("Cooking Complete", TimerTimesOut.class);

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

* `MicrowaveStateMachine` - throw events at this to see transitions and to collect signals to self and others emitted during the new state's onEntry behaviour
* `MicrowaveBehaviour` - interface to capture onEntry procedures
* `MicrowaveBehaviourBase` - a do-nothing implentation of `MicrowaveBehaviour` that is convenient for inheriting

The cleanest implementation of this generation is using maven artifacts.

Create a maven artifact like [state-machine-test-definition](state-machine-test-definition) and customize `StateMachines.java`.

Then in the project where you want to have the generated classes, set it up like [state-machine-test](state-machine-test). That means adding dependencies on your definition artifact to the pom and also snippets using *state-machine-maven-plugin* and *build-helper-maven-plugin* to your pom.xml. Then you will have generated state machine classes to use in your program.

Behaviour
---------------
When a transition occurs in a state machine from state A to state B, the transition is not considered complete till the *onEntry* procedure for B has been run. Behaviour is specified according to a generated interface and is given to an instance of `MicrowaveStateMachine` at creation. For instance to specify that when a Microwave enters the Cooking state that it will time out and stop cooking after 30 seconds (transition to state Cooking Complete) we would implement the behaviour for a Microwave like this:

```java
MicrowaveBehaviour behaviour = new MicrowaveBehaviourBase() {
    @Override
    public Microwave onEntry_Cooking(Signaller signaller, Microwave microwave,
            ButtonPressed event) {
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

	<T> void signal(T object, Event<?> event);
	
	<T> void signal(T object, Event<?> event, long delay, TimeUnit unit);
	
    void cancelSignal(Object from , Object to);
}
```

When the *onEntry* procedure is run all signals to self and to others are collected. Upon completion the signals to self are run first in order they were called in the procedure and then the signals to others are run.

At any one time there should only be one outstanding scheduled (non-immediate) signal between object 1 and object 2. This is clearly a pre-requesite for `cancelSignal` to make sense in its present form and is a nice simplification generally.

Signals to self are actioned synchronously but signals to others may be actioned asynchronously.

Rx processing of signals
-------------------------
The runtime artifact has optional support for a reactive implementation of the processing of signals using [RxJava](https://github.com/ReactiveX/RxJava).




