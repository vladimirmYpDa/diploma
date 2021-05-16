package com.diploma.app.genericadmin;

public interface IEntity<T> {
    T getId();
    void setId(T id);
}
