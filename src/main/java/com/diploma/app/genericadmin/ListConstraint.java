package com.diploma.app.genericadmin;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class ListConstraint<T> implements IFieldConstraint {
    protected List<T> values;
}
