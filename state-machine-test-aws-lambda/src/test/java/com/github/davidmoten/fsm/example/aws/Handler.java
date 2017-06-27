package com.github.davidmoten.fsm.example.aws;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.internal.IteratorSupport;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.github.davidmoten.fsm.example.generated.MicrowaveBehaviour;
import com.github.davidmoten.fsm.example.generated.MicrowaveStateMachine;
import com.github.davidmoten.fsm.example.generated.MicrowaveStateMachine.State;
import com.github.davidmoten.fsm.example.microwave.Microwave;
import com.github.davidmoten.fsm.example.microwave.event.DoorOpened;
import com.github.davidmoten.fsm.runtime.Signal;
import com.github.davidmoten.fsm.runtime.rx.ClassId;

public class Handler {

    public String submitSignal(Map<String, Object> input, Context context) {
        LambdaLogger log = context.getLogger();

        String body = (String) input.get("body");
        log.log("body=" + body);
        return "ok";
    }

    public String process(Map<String, Object> input, Context context) {

        // use fifo sqs queue with groupId equal to Class+Id
        //
        // to support fifo deduplication every signal needs to include a from
        // Class+Id, eventId pair in the serialized signal
        //
        // get the latest state bytes from the item in Entity table identified
        // by the msg entityId
        // deserialize the bytes into an an entity object and its state enum
        //
        // create an EntityStateMachine object intialized with entity object and
        // its state
        //
        // apply the Event object to the EntityStateMachine object
        // serialize the new state to bytes
        //
        // fifo queue regions still limited so use a US region
        AmazonSQS sqs = AmazonSQSClientBuilder //
                .standard() //
                .withRegion(Regions.US_WEST_2) //
                .build();
        String signalQueueUrl = sqs //
                .getQueueUrl("signal-queue.fifo") //
                .getQueueUrl();
        List<Message> msgs = readQueue(sqs, signalQueueUrl);
        if (msgs.isEmpty()) {
            return "none";
        } else {
            AmazonDynamoDB dbClient = AmazonDynamoDBClientBuilder //
                    .standard() //
                    .withRegion(Regions.AP_SOUTHEAST_2) //
                    .build();
            DynamoDB db = new DynamoDB(dbClient);
            Table entityTable = db.getTable("Entity");
            for (Message msg : msgs) {
                Signal<Microwave, String> signal = parseSignal(msg.getBody());
                String entityId = signal.cls().getCanonicalName() + ":" + signal.id();
                Item item = entityTable.getItem("EntityId", entityId);
                MicrowaveBehaviour<String> behaviour = createMicrowaveBehaviour();
                Microwave m = Microwave.fromId(signal.id());
                String id = null;
                State state = MicrowaveStateMachine.State.COOKING;
                MicrowaveStateMachine<String> sm = MicrowaveStateMachine.create(m, id, behaviour,
                        state);
                sm.signal(signal.event());
                // send signals to others
                for (Signal<?, ?> sig : sm.signalsToOther()) {
                    // serialize sig to bytes
                    // put bytes on queue
                    String serialized = Base64.getEncoder().encodeToString(serialize(sig));
                    SendMessageRequest req = new SendMessageRequest() //
                            .withQueueUrl(signalQueueUrl) //
                            .withMessageBody(serialized) //
                            .withMessageGroupId(toGroupId(sig));
                    sqs.sendMessage(req);
                }
                Microwave m2 = sm.get().get();
                // TODO serialize m2 and save in entityTable item
                byte[] bytes = serialize(m2);
                Item item2 = new Item() //
                        .with("EntityId", entityId) //
                        .withBinary("entityBytes", bytes);
                entityTable.putItem(item2);
            }
        }
        return "ok";

    }

    private byte[] serialize(Microwave m2) {
        // TODO
        return new byte[] { 1, 2, 3 };
    }

    private static String toGroupId(Signal<?, ?> sig) {
        return sig.cls().getCanonicalName() + ":" + sig.id();
    }

    private static byte[] serialize(Signal<?, ?> sig) {
        return new byte[] { 1, 2, 3 };
    }

    private static Signal<Microwave, String> parseSignal(String body) {
        return Signal.create(ClassId.create(Microwave.class, "1"), new DoorOpened());
    }

    private MicrowaveBehaviour<String> createMicrowaveBehaviour() {
        // TODO Auto-generated method stub
        return null;
    }

    private void updateStuff(DynamoDB db, String entityId) {

    }

    private void replay(DynamoDB db, String entityId) {
        Table entityEvent = db.getTable("EntityEvent");
        QuerySpec spec = new QuerySpec() //
                .withKeyConditionExpression("EntityId = :entityId")
                .withValueMap(new ValueMap().withString("entityId", entityId));
        ItemCollection<QueryOutcome> items = entityEvent.query(spec);
        IteratorSupport<Item, QueryOutcome> it = items.iterator();
        while (it.hasNext()) {
            Item item = it.next();
            // TODO push through Processor
        }
    }

    private static List<Message> readQueue(AmazonSQS sqs, String signalQueueUrl) {
        ReceiveMessageRequest req = new ReceiveMessageRequest() //
                .withQueueUrl(signalQueueUrl) //
                .withMaxNumberOfMessages(1);
        ReceiveMessageResult resp = sqs.receiveMessage(req);
        List<Message> msgs = resp.getMessages();
        return msgs;
    }

}
