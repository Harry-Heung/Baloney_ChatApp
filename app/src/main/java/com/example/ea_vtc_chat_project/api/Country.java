package com.example.ea_vtc_chat_project.api;

public class Country {
    private Name name;

    public Name getName() {
        return name;
    }

    public void setName(Name name) {
        this.name = name;
    }

    public static class Name {
        private String common;

        public String getCommon() {
            return common;
        }

        public void setCommon(String common) {
            this.common = common;
        }
    }
}
