package com.helospark.financialdata;

import jakarta.annotation.Generated;

record TestRecord(String name, String address) {

    @Generated("SparkTools")
    private TestRecord(Builder builder) {
        this(builder.name, builder.address);
    }

    @Generated("SparkTools")
    public static Builder builder() {
        return new Builder();
    }

    @Generated("SparkTools")
    public static final class Builder {
        private String name;
        private String address;

        private Builder() {
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withAddress(String address) {
            this.address = address;
            return this;
        }

        public TestRecord build() {
            return new TestRecord(this);
        }
    }

}

class TestClass {
    private String name;
    private String address;

    @Generated("SparkTools")
    private TestClass(Builder builder) {
        this.name = builder.name;
        this.address = builder.address;
    }

    public TestClass(String name, String address) {
        this.name = name;
        this.address = address;
    }

    @Generated("SparkTools")
    public static Builder builder() {
        return new Builder();
    }

    @Generated("SparkTools")
    public static final class Builder {
        private String name;
        private String address;

        private Builder() {
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withAddress(String address) {
            this.address = address;
            return this;
        }

        public TestClass build() {
            return new TestClass(this);
        }
    }

}

public class RecordMain {
    public static void main(String[] args) {
        var asd = new TestRecord("name", "address");
        var asd2 = new TestClass("name", "address");

        var sd = TestRecord.builder().withName("name").withAddress("address").build();

        System.out.println(sd);
    }
}