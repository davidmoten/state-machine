package com.github.davidmoten.fsm.persistence;

public interface Sql {

    public static final Sql DEFAULT = new Sql() {
    };

    default String addToSignalQueue() {
        return "insert into signal_queue(cls, id, event_cls, event_bytes) values(?,?,?,?)";
    }

    default String selectDelayedSignals() {
        return "select seq_num, cls, id, event_cls, event_bytes, time from delayed_signal_queue order by seq_num";
    }

    default String delayedSignalExists() {
        return "select 0 from delayed_signal_queue where seq_num=?";
    }

    default String signalExists() {
        return "select 0 from signal_queue where seq_num=?";
    }

    default String readEntityAndState() {
        return "select state, bytes from entity where cls=? and id=?";
    }

    default String addToSignalStore() {
        return "insert into signal_store(cls, id, event_cls, event_bytes) values(?,?,?,?)";
    }

    default String deleteDelayedSignal() {
        return "delete from delayed_signal_queue where from_cls=? and from_id=? and cls=? and id=?";
    }

    default String addDelayedSignal() {
        return "insert into delayed_signal_queue(from_cls, from_id, cls, id, event_cls, event_bytes, time) values(?,?,?,?,?,?,?)";
    }

    default String deleteNumberedSignal() {
        return "delete from signal_queue where seq_num=?";
    }

    default String deleteNumberedDelayedSignal() {
        return "delete from delayed_signal_queue where seq_num=?";
    }

    default String updateEntity() {
        return "update entity set bytes=?, state=? where cls=? and id=?";
    }

    default String insertEntity() {
        return "insert into entity(cls, id, bytes, state) values(?,?,?,?)";
    }

    default String readEntity() {
        return "select bytes from entity where cls=? and id=?";
    }

	default String readAllEntities() {
		return "select id, bytes from entity where cls=?";
	}

}
