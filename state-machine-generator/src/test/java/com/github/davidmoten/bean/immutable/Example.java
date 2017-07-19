package com.github.davidmoten.bean.immutable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.davidmoten.bean.annotation.NonNull;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

public class Example implements Serializable {

    @NonNull
    private final String id;
    private final int number;
    private final Date[] values;

    @JsonCreator
    Example(
      @JsonProperty("id") String id,
      @JsonProperty("number") int number,
      @JsonProperty("values") Date[] values) {
        this.id = id;
        this.number = number;
        this.values = values;
    }

    public String id() {
        return id;
    }

    public int number() {
        return number;
    }

    public Date[] values() {
        return values;
    }

    public Example withId(String id) {
        return new Example(id, number, values);
    }

    public Example withNumber(int number) {
        return new Example(id, number, values);
    }

    public Example withValues(Date[] values) {
        return new Example(id, number, values);
    }

    // Constructor synchronized builder pattern.
    // Changing the parameter list in the source
    // and regenerating will provoke compiler errors
    // wherever the builder is used.

    public static Builder2 id(String id) {
        Builder b = new Builder();
        b.id = id;
        return new Builder2(b);
    }

    private static final class Builder {
        String id;
        int number;
        Date[] values;
    }

    public static final class Builder2 {

        private final Builder b;

        Builder2(Builder b) {
            this.b = b;
        }

        public Builder3 number(int number) {
            b.number = number;
            return new Builder3(b);
        }
    }

    public static final class Builder3 {

        private final Builder b;

        Builder3(Builder b) {
            this.b = b;
        }

        public Example values(Date[] values) {
            b.values = values;
            return new Example(b.id, b.number, b.values);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, number, values);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (!(o instanceof Example)) {
            return false;
        } else {
            Example other = (Example) o;
            return
                Objects.deepEquals(this.id, other.id)
                && Objects.deepEquals(this.number, other.number)
                && Objects.deepEquals(this.values, other.values);
        }
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("Example[");
        b.append("id=" + this.id);
        b.append(",");
        b.append("number=" + this.number);
        b.append(",");
        b.append("values=" + this.values);
        b.append("]");
        return b.toString();
    }
}
