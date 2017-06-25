package com.github.davidmoten.fsm.example.aws;

import java.util.List;
import java.util.Map;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Expected;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.internal.IteratorSupport;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

public class Handler {

    private static final long THRESHOLD_MS = 60000;

    public String submitSignal(Map<String, Object> input, Context context) {
        LambdaLogger log = context.getLogger();

        String body = (String) input.get("body");
        log.log("body=" + body);
        return "ok";
    }

    public String process(Map<String, Object> input, Context context) {

        // for each message on the queue while time allows
        // deserialize the signal message to extract Class, Id and Event objects
        // atomically set the time field on the entity if 0 or too old as a
        // work-in-progress counter.

        // if the time field was set then
        // get the latest state bytes from the item in Entity table identified
        // by the msg entityId
        // deserialize the bytes into an an entity object and its state enum
        // create an EntityStateMachine object
        //
        // apply the Event object to the EntityStateMachine object
        // serialize the new state to bytes 
        AmazonSQS sqs = AmazonSQSClientBuilder //
                .standard() //
                .withRegion(Regions.AP_SOUTHEAST_2) //
                .build();
        String signalQueueUrl = sqs //
                .getQueueUrl("signal-queue") //
                .getQueueUrl();
        List<Message> msgs = readQueue(sqs, signalQueueUrl);
        if (msgs.isEmpty()) {
            return "none";
        } else {
            for (Message msg : msgs) {
                // TODO extract from msg
                String entityId = "microwave:1";
                AmazonDynamoDB dbClient = AmazonDynamoDBClientBuilder //
                        .standard() //
                        .withRegion(Regions.AP_SOUTHEAST_2) //
                        .build();
                DynamoDB db = new DynamoDB(dbClient);
                Table entityTable = db.getTable("Entity");
                Item entity;
                while (true) {
                    long now = System.currentTimeMillis();
                    entity = entityTable.getItem("EntityId", entityId);
                    if (entity == null) {
                        entity = new Item().with("EntityId", entityId).with("time", now);
                        try {
                            entityTable.putItem(entity, new Expected("EntityId").notExist());
                            // put succeeded
                            break;
                        } catch (ConditionalCheckFailedException e) {
                            // do nothing, loop again
                        }
                    } else {
                        break;
                    }
                }
                while (true) {
                    long now = System.currentTimeMillis();
                    long time = entity.getLong("time");
                    if (time == 0) {
                        Item item = new Item().with("EntityId", entityId).with("time", now);
                        if (putItem(entityTable, item, new Expected("time").eq(0L))) {
                            updateStuff(db, entityId);
                            break;
                        }
                    } else if (now - time > THRESHOLD_MS) {
                        Item item = new Item().with("EntityId", entityId).with("time", now);
                        if (putItem(entityTable, item, new Expected("time").eq(time))) {
                            updateStuff(db, entityId);
                            break;
                        }
                    } else {
                        Item item = new Item().with("EntityId", entityId).with("time", time);
                        if (putItem(entityTable, item, new Expected("time").eq(time))) {
                            // don't update anything
                            return "";
                        }
                    }
                }
            }
        }
        return "ok";
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

    private static boolean putItem(Table table, Item item, Expected expected) {
        try {
            table.putItem(item, expected);
            return true;
        } catch (ConditionalCheckFailedException e) {
            return false;
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
