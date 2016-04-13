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

Example State Diagram
------------------------
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
As you can see the definition is very concise. This is largely because of the advisable constraint that any one State can only be arrived at via one Event type. Note that you can still use inheritance of the Event if you wish so that for say a `Position` event you could pass the state machine a `PositionWithSpeed` event or a `SimplePosition event` as long as they both inherit from `Position`.

`a.to(b)` records that there is a transition from `a -> b` and returns `b`.

`a.from(b)` records there is a transition from `b -> a` and returns `a`.  

Generating code
----------------

From the above state machine definition for a microwave we generate these classes:

* `MicrowaveStateMachine` - throw events at this to see transitions
* `MicrowaveBehaviour` - interface to capture onEntry procedures
* `MicrowaveBehaviourBase` - a do-nothing implentation of `MicrowaveBehaviour` that is convenient for inheriting

The cleanest implementation of this generation is using maven artifacts.

Create a maven artifact like [state-machine-test-definition](state-machine-test-definition) and customize `StateMachines.java`.

Then in the project where you want to have the generated classes, set it up like [state-machine-test](state-machine-test). That means adding dependencies on your definition artifact to the pom and also snippets using *state-machine-maven-plugin* and *build-helper-maven-plugin* to your pom.xml. Then you will have generated state machine classes to use in your program.




