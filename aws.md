How could CQRS be used on AWS?
---------------------------------
Specifically how can scalable processing of the Command part of CQRS be achieved?

## Components

* SQS - *signalQueue*, queues for every class and id created on demand (eventListQueue)
* SNS - *signalTopic*
* Lambda - *processorLambda*
* API Gateway - *signalResource*
* S3 - event lists by class and id (need a text format that can contain a list of events)
* *signalResource* puts a text signal on *signalQueue* and then sends RUN to *signalTopic*
* *processorLambda* is subscribed to *signalTopic*
s
## processorLambda

processorLambda does the following:

```
get first signal from signalQueue
if no message then exit

eventListQueue = get eventListqueue for class and id
if eventListQueue does not exist then 
  create sqs queue (e.g. systemprefix-Microwave-1)
  eventList = new empty list
else 
  get first s3id from eventListQueue
  if does not exist then exit
  eventList = read event list from S3 using s3Id
  replay eventList through state-machine Processor
  push signal.event through Processor
  for (each signalToOther from Processor) 
    call signalResource with signalToOther
  newEventList = append signal.event to eventList
  newS3Id = save newEventList to new s3 object
  # update for read (eventually consistent)
  update s3 object for the entity to newS3id
  put newS3Id on eventListQueue
  remove s3Id from eventListQueue
  remove signal from signalQueue
```

