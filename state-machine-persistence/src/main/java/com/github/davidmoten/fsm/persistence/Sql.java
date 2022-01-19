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

    default String deleteEntityProperties() {
        return "delete from entity_property where cls=? and id=?";
    }

    default String insertEntityProperty() {
        return "insert into entity_property(cls, id, name, \"value\") values(?,?,?,?)";
    }

    default String readEntitiesByProperty() {
        return "select id, bytes from entity\n"//
                + "where id in (\n" //
                + "select id from entity_property\n" //
                + "where cls=? and name=? and \"value\"=?" //
                + ")";
    }

    default String readEntitiesByPropertyAndRange(boolean startInclusive, boolean endInclusive) {
        return "select id, bytes from entity\n"//
                + "where id in (\n" //
                + "select id from entity_prop_range_int\n" //
                + "where cls=? and name=? and \"value\"=?\n" //
                + "and range_name=?\n" //
                + "and range_value" + (startInclusive ? ">=" : ">") + "?\n"//
                + "and range_value" + (endInclusive ? "<=" : "<") + "?\n"//
                + "order by range_value, id\n" //
                + "limit ?" //
                + ")";
    }

    default String deleteEntityRangeProperties() {
        return  "delete from entity_prop_range_int where cls=? and id=?";
    }

    default String insertEntityRangeProperty() {
        return "insert into entity_prop_range_int(cls, id, name, \"value\", range_name, range_value) values(?,?,?,?,?,?)";
    }

}
