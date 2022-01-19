state-machine
==============
<a href="https://github.com/davidmoten/state-machine/actions/workflows/ci.yml"><img src="https://github.com/davidmoten/state-machine/actions/workflows/ci.yml/badge.svg"/></a><br/>
[![codecov](https://codecov.io/gh/davidmoten/state-machine/branch/master/graph/badge.svg)](https://codecov.io/gh/davidmoten/state-machine)<br/>
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.davidmoten/state-machine-runtime/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/com.github.davidmoten/state-machine-runtime)<br/>

Generates java classes to handle state transitions based on a state machine defined with type safety in java. Supports 
immutability (though is not opinionated in the sense that if you want to mutate your objects you can).

* Concise API using method chaining
* leverages the simplicity of the Executable UML approach
to state diagrams (one Event type for each State)
* Maven plugin
* Not coupled to a storage mechanism (both a feature and a non-feature!)
* optional reactive API using [RxJava 2](https://github.com/ReactiveX/RxJava) (very useful for asynchronous coordination and for extensions like storage if desired)
* optional relational database persistence module 

Status: *beta*

Example State Diagram
------------------------
Consider a microwave. If you were going to write the control system for a microwave you'd find it's a natural candidate for a state machine (actually all programming is working with state machines but we are going to work with one quite explicitly). We are going to work with a very simple microwave design (one you'd be crazy to buy in the shops!) just to demonstrate state diagrams.

<img src="src/docs/microwave-state-diagram.png?raw=true" />

The definition looks like this:

```java
// create state machine 
StateMachine<Microwave> m = 
    StateMachine.create(Microwave.class);

// create states
State<Microwave, DoorClosed> readyToCook = 
    m.createState("Ready to Cook", DoorClosed.class);
State<Microwave, DoorOpened> doorOpen = 
    m.createState("Door Open", DoorOpened.class);
State<Microwave, ButtonPressed> cooking = 
    m.createState("Cooking", ButtonPressed.class);
State<Microwave, DoorOpened> cookingInterruped = i
    m.createState("Cooking Interrupted", DoorOpened.class);
State<Microwave, TimerTimesOut> cookingComplete = i
    m.createState("Cooking Complete", TimerTimesOut.class);

// define transitions
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
MicrowaveBehaviour<String> behaviour = new MicrowaveBehaviourBase<String>();
MicrowaveStateMachine<String> m = 
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
Every object controlled by a state machine must have an identifier unique by `object.getClass()`. When an immutable object is transformed by a state transition then the transformed object must have the same id as the original object.

Generating a diagram
-------------------------
It's a great idea to generate a diagram from what you have coded to ensure it is what you expect. In the final product you might choose to unit test all transitions but while you are exploring the requirements and your design it's really useful to visualize the state machines your are creating. 

After numerous experiments over the years I've settled on generating a [GraphML](http://graphml.graphdrawing.org/) file and using the excellent free 
tool [yEd](https://www.yworks.com/products/yed) to automate the layout. The state-machine maven plugin generates code but also generates `.graphml` files (with some *yEd* extensions) for each state machine that can be opened in *yEd*. Select **Layout - Orthogonal - UML Style** (Alt-Shift-U) and a dialog will appear. The setting `Grid` in the dialog affects the internode spacing so play with that as you wish. I usually set (just once) the setting **Labeling - Edge Label Model - Side Slider** to avoid ambiguity in positioning of edge labels. The results are excellent!

<img src="state-machine-test/src/docs/com.github.davidmoten.fsm.example.microwave.Microwave.png?raw=true" />

The state-machine maven plugin also generates a more detailed state diagram that includes documentation of each state in the diagram nodes. This is how html documentation is associated with each state:

```java
State<Microwave, DoorClosed> readyToCook = 
    m.createState("Ready to Cook", DoorClosed.class)
     .documentation("<pre>entry/\nturn light off;</pre>");
```

In the example below the documentation is a pseudo-code description of the *entry procedures* for each state (discussed in [Behaviour](https://github.com/davidmoten/state-machine#behaviour) section below):

<img src="state-machine-test/src/docs/com.github.davidmoten.fsm.example.microwave.Microwave-with-docs.png?raw=true" />

Here's a more complex one:

<img src="src/docs/complex-state-diagram.png?raw=true" />

On my linux machine I have a command line alias for *yEd* 

```bash
alias yed='java -jar /opt/yed/current/yed.jar'
``` 

so that I can automate the regeneration of the diagram from the command line like this:

```bash
mvn clean install && yed state-machine/state-machine-test/target/state-machine-docs/com.github.davidmoten.fsm.example.microwave.Microwave.graphml
```

Hit `Alt-Shift-U` to do the layout. A dialog appears called *Directed Orthogonal Layout*. A favourite setting is to change **Labelling - Edge Labelling** to *Integrated*. Then export the diagram as you please (**File - Export**).

Behaviour
---------------
When a transition occurs in a state machine from state A to state B, the transition is not considered complete till the *entry procedure* for B has been run. Behaviour is specified according to a generated interface and is given to an instance of `MicrowaveStateMachine` at creation. For instance to specify that when a Microwave enters the Cooking state that it will time out and stop cooking after 30 seconds (transition to state Cooking Complete) we would implement the behaviour for a Microwave like this:

```java
MicrowaveBehaviour<String> behaviour = new MicrowaveBehaviourBase<String>() {
    @Override
    public Microwave onEntry_Cooking(Signaller<Microwave, String> signaller, Microwave microwave,
            String id, ButtonPressed event, boolean replaying) {
        signaller.signalToSelf(new TimerTimesOut(), 30, TimeUnit.SECONDS);
        return microwave;
    }
};
```

The signaller has these methods:

```java
public interface Signaller<T, Id> {

    void signalToSelf(Event<? super T> event);
    
    void signalToSelf(Event<? super T> event, long delay, TimeUnit unit);

    <R> void signal(Class<R> cls, Id id, Event<? super R> event);
	
    <R> void signal(Class<R> cls, Id id, Event<? super R> event, long delay, TimeUnit unit);
	
    void cancelSignal(Class<?> fromClass, Id fromId, Class<?> toClass, Id toId);

    void cancelSignalToSelf();

    long now();

}
```

When the *entry procedure* is run all signals to self and to others are collected. Once the entry procedure completes the signals to self are actioned each of which may invoke a transition that sends more signals. Signals to self are invoked synchronously but the signals to others are collected in a queue till all transitions due to signals to self have completed. The signals to others are then invoked (usually asynchronously though signals to the same entity will be serial).

At any one time there should only be one outstanding scheduled (non-immediate) signal between object 1 and object 2. This is clearly a pre-requisite for `cancelSignal` to make sense in its present form and is a nice simplification generally.

Signals to self are actioned synchronously but signals to others may be actioned asynchronously.

You may note that a parameter passed to each behaviour method is `replaying`. When replaying events you should not make any calls to external entities. Any calls to other entities using `signaller` will be automatically suppressed but if in the entry procedure you make a call to `service.sendEmail(...)` for instance then you should check the value of `replaying` before calling it:

```java 
if (!replaying) {
    service.sendEmail(...);
}
```

Rx processing of signals
-------------------------
The runtime artifact has support by default for a reactive implementation of the processing of signals using [RxJava](https://github.com/ReactiveX/RxJava).

If you don't need rx support then add an exclude to your maven dependency.

Processing signals in parallel demands that entities are thread-safe. Immutability is recommended for this case.

[StreamingTest.java](state-machine-test/src/test/java/com/github/davidmoten/fsm/rx/StreamingTest.java) demonstrates usage of an Rx processor for a state machine (or set of state machines).

Persistence
---------------
For a system to recover properly from failure all signals and state changes should be persisted to media that survives process restarts.

If signal queues and entity state and optionally signal stores are all kept in a single relational database that supports transactions then *exactly once* messaging can be guaranteed. For a reaonably sized system without the need to scale massively this is the recommended approach. 

If you can relax the consistency requirements of your system then you can use *eventually consistent* storage with the consequence that the system is now *at least once* in terms of message processing. If you can deal sensibly with multiple deliveries of the same message then you can introduce non-RDB systems for storage that may scale easier than RDB.

A popular architectural pattern for handling persistence is to use [Event Sourcing](http://martinfowler.com/eaaDev/EventSourcing.html) and [CQRS](http://martinfowler.com/bliki/CQRS.html). 

Event Sourcing and CQRS and Guaranteed Delivery
----------------------------------------
The term *Event Sourcing* might be better described as *Signal Sourcing* when one thinks of a system of state machines rather than a single state machine.

Incoming signals to a system are placed on a *Command Queue* and then processed. The diagram below indicates the path taken on restart of a system. 
For each domain object state machines need to be refreshed from the *Event Source* (which we are calling a *Signal Store*).
<br/>
<br/>
<img src="src/docs/collaboration-diagram.png?raw=true" />

Relational Database Persistence
--------------------------------
The *state-machine-persistence* module provides everything you need to implement Event Sourcing with a relational database (RDB).

When you use a relational database then the changes to a state machine due to a signal are surrounded by a database transaction. This means that if some failure occurs in the processing of that event then the state change does not happen and any signals arising from that change are not sent.

Here's an example using the `Persistence` class:

```java
Persistence p = Persistence //
    .connectionFactory(connectionFactory) //
    .behaviour(Account.class, new AccountBehaviour()) //
    .build();
// load all outstanding signals from the database    
p.initialize();
// send signals to the system
p.signal(Account.class, "1", new Create());
p.signal(Account.class, "1", new Deposit(BigDecimal.valueOf(100)));
p.signal(Account.class, "1", new Transfer(BigDecimal.valueOf(12), "2"));
```
Note that when you use a `Persistence` class to route signals that you can do database lookups in your behaviour implementations using the `Entities` object that will be loaded with necessary context via `ThreadLocal`. Here's an example taken from the shopping example application ([CatalogProductBehaviour.java](src/main/java/shop/behaviour/CatalogProductBehaviour.java)):

```java
@Override
public CatalogProduct onEntry_Created(Signaller<CatalogProduct, String> signaller, String id, Create event,
        boolean replaying) {
    // lookup product within the transaction
    Optional<Product> product = Entities.get().get(Product.class, event.productId());
    if (product.isPresent()) {
        return CatalogProduct.createWithCatalogId(event.catalogId()) //
                .productId(event.productId()) //
                .name(product.get().name()) //
                .description(product.get().description()) //
                .quantity(event.quantity()) //
                .price(event.price()) //
                .tags(product.get().tags());
    } else {
        throw new RuntimeException("product not found " + event.productId());
    }
}
```

Note that in the above example the *replaying* option is not supported. In theory when replaying no interaction with external services should occur and the `Entities` service is one such. To support Event Sourcing with replay any interaction with an external service would be made via asynchronous signals so that the return value from the service is recorded in the Signal Store.

Because the database structure is strongly abstracted you need to manage lookup performance (outside of entity primary keys) via the use of *tags* on entities. All tags are indexed to enable fast queries. 

TODO: discuss range metrics and use with tags

### Shopping example application

The module *state-machine-example-shopping* (and its dependency *state-machine-example-shopping-definition*) contain a fully working web application with these features:

* Spring Boot MVC web application
* Rest API (not used by MVC but available anyway)
* Uses an in-memory H2 database (disappears on shutdown)
* Data model generated as immutable classes with many utility methods which add ease and safety for programming state transition behaviour
* all changes to data go through generated `StateMachine` instances

To run the shopping webapp:

```bash
mvn clean install
cd state-machine-example-shopping
mvn spring-boot:run
```
Then go to [http://localhost:8080](http://localhost:8080).

You can also run this app in Eclipse or another IDE by importing the *state-machine-example-shopping* module as a Maven Project and running the Main class `shop.Application`.

Highly scalable persistence
--------------------------

A scalable implementation of an Event Sourcing + CQRS architecture might use:

* AWS SQS for the *Command Queue*
* Apache Kafka for the *Signal Store*

To leverage the performance benefits of eventual consistency the state machines must be designed so
that *at least once* processing of events does not break business logic. Not every problem will be suited to this but many scenarios can be solved this way. Remember to consider:

* code defensively in expectation of more-than-once events
* consider probabilities of more-than-once delivery to state machines with critical roles 
* consider using consistent write/reads to the data stores for state machines with critical roles
* partition parts of the system to use RDB so that they are accessed transactionally to support *exactly once* message processing

A basic implementation of the architecture might use:

* JSON for deserialization
* *BigQueue* for the *Command Queue*
* Local flat files per state machine for the *Event Stores* (together comprising a *Signal Store*)






