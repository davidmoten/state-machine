create table signal_queue (
  seq_num identity,
  cls varchar(512) not null,
  id  varchar(255) not null,
  event_cls varchar(512) not null, 
  event_bytes blob not null,
  primary key(seq_num)
);
  
create table delayed_signal_queue (
  cls varchar(512) not null,
  id  varchar(255) not null,
  time timestamp not null,
  event_cls varchar(512) not null, 
  event_bytes blob not null,
  primary key(cls, id)
);

create table entity (
  cls varchar(512) not null, 
  id varchar(255) not null,
  state varchar(255) not null,
  bytes blob not null,
  primary key(cls, id)
);
   
create table signal_store (
  seq_num identity,
  cls varchar(512) not null, 
  id varchar(255) not null,
  event_cls varchar(512) not null, 
  event_bytes blob not null,
  primary key(seq_num)
); 
   
 create index idx_sig_store on signal_store(cls, id, seq_num);
 
 create index idx_delayed_sig_q_time on delayed_signal_queue(time); 
    
  