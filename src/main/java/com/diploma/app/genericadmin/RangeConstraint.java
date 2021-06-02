package com.diploma.app.genericadmin;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class RangeConstraint<T> implements IFieldConstraint {
    protected T min;
    protected T max;
}
