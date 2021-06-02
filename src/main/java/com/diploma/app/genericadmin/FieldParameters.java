package com.diploma.app.genericadmin;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class FieldParameters {
    protected String fieldName;
    protected String displayName;
    protected String type;
    protected boolean required;
    protected IFieldConstraint constraint;

    public FieldParameters(String fieldName, String displayName, String type, boolean required) {
        this.fieldName = fieldName;
        this.displayName = displayName;
        this.type = type;
        this.required = required;
    }
}
