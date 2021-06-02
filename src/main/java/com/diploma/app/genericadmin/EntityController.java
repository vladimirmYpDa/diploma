package com.diploma.app.genericadmin;

import com.diploma.app.model.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.GenericTypeResolver;
import org.springframework.data.repository.CrudRepository;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class EntityController<EntityType extends IEntity<Long>> {

    /**
     * Supplementary set of classes used to check if field is numeric.
     */
    private static final Set<Class<?>> primitiveNumbers = Stream
            .of(int.class, long.class, float.class, double.class, byte.class, short.class)
            .collect(Collectors.toSet());

    /**
     * Contains data describing fields of a particular entity's admin interface.
     */
    protected final LinkedHashMap<Field, FieldParameters> fields = new LinkedHashMap<>();

    /**
     * Workaround: generic type class field because Java
     * doesn't allow to reference generic type in the code.
     */
    protected final Class<EntityType> genericType;

    @Autowired
    protected CrudRepository<EntityType, Long> entityRepository;

    public EntityController() {
        this.genericType = (Class<EntityType>) GenericTypeResolver.resolveTypeArgument(
                getClass(), EntityController.class);

        Arrays.stream(genericType.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(DisplayParameters.class))
                .sorted(Comparator.comparing(field -> field.getAnnotation(DisplayParameters.class).order()))
                .forEachOrdered(field -> {
                    DisplayParameters a = field.getAnnotation(DisplayParameters.class);

                    boolean required = field.isAnnotationPresent(NotBlank.class) || field.isAnnotationPresent(NotNull.class);
                    FieldParameters params = new FieldParameters(field.getName(), a.name(),
                            getTemplateInputType(field), required);
                    fields.put(field, params);
                });
    }

    public boolean isNumeric(Field field) {
        Class<?> c = field.getType();
        if (c.isPrimitive())
            return primitiveNumbers.contains(c);
        else
            return Number.class.isAssignableFrom(c);
    }

    /**
     * Override me and call super method to
     * specify custom types for template inputs.
     * @param field Class field to get type for
     * @return Template input type
     */
    public String getTemplateInputType(Field field) {
        Class<?> c = field.getType();

        if (c.isEnum())
            return "select";

        if (isNumeric(field))
            return "number";

        if (c == Date.class)
            return "date";

        return "text";
    }

    /**
     * Override me and call super method to specify
     * custom types for template table filters.
     * @param field Class field to get type for
     * @return Template table filter data type
     */
    public String getTemplateFilterDataType(Field field) {
        Class<?> c = field.getType();

        if (isNumeric(field))
            return "number";

        if (c == Date.class)
            return "date";

        return "string";
    }

    /**
     * Gets called to determine dynamically changing constraints at runtime.
     * Override me to specify custom constraints for template inputs.
     * @param field Class field to evaluate constraint for
     * @return Constraint for this field
     */
    public IFieldConstraint getConstraint(Field field) {
        if (field.getType().isEnum())
            return new ListConstraint<>(Arrays.asList(field.getType().getEnumConstants()));

        if (isRangeField(field))
            return new RangeConstraint<>(getFieldMin(field), getFieldMax(field));

        return null;
    }

    public boolean isRangeField(Field field) {
        return field.isAnnotationPresent(Min.class) || field.isAnnotationPresent(Max.class)
            || field.isAnnotationPresent(DecimalMin.class) || field.isAnnotationPresent(DecimalMax.class);
    }

    public BigDecimal getFieldMin(Field field) {
        if (field.isAnnotationPresent(Min.class))
            return BigDecimal.valueOf(field.getAnnotation(Min.class).value());

        if (field.isAnnotationPresent(DecimalMin.class))
            return new BigDecimal(field.getAnnotation(DecimalMin.class).value());

        return null;
    }

    public BigDecimal getFieldMax(Field field) {
        if (field.isAnnotationPresent(Max.class))
            return BigDecimal.valueOf(field.getAnnotation(Max.class).value());

        if (field.isAnnotationPresent(DecimalMax.class))
            return new BigDecimal(field.getAnnotation(DecimalMax.class).value());

        return null;
    }

    public String getEntityName() {
        return genericType.getAnnotation(EntityName.class).value();
    }

    protected void setGenericInfo(ModelMap model) {
        model.put("entitiesName", this.getEntityName());
        fields.forEach((key, value) -> value.setConstraint(getConstraint(key)));
        model.put("fields", fields);
    }

    //region Controller binding and mappings

    @InitBinder
    public void setAllowedFields(WebDataBinder dataBinder) {
        dataBinder.setDisallowedFields("id");
    }

    @GetMapping
    public String displayList(ModelMap model) {
        setGenericInfo(model);
        model.put("objs", entityRepository.findAll());

        return "admin/list";
    }

    @GetMapping("new")
    public String initCreate(ModelMap model) {
        setGenericInfo(model);

        Node obj = new Node();
        model.put("obj", obj);
        model.put("newObj", true);

        return "admin/update";
    }

    @PostMapping("new")
    public String processCreate(ModelMap model, @ModelAttribute("obj") @Valid EntityType obj,
                                BindingResult result, RedirectAttributes redirectAttributes)
    {
        if (result.hasErrors()) {
            setGenericInfo(model);
            model.put("newObj", true);
            return "admin/update";
        } else {
            entityRepository.save(obj);
            redirectAttributes.addFlashAttribute("successMessage", "Запись успешно создана!");
            return "redirect:.";
        }
    }

    @GetMapping("update")
    public String initUpdate(@RequestParam(value = "id") Long id, ModelMap model) {
        Optional<EntityType> obj = entityRepository.findById(id);
        if (obj.isPresent()) {
            setGenericInfo(model);
            model.put("obj", obj.get());
            model.put("newObj", false);
            return "admin/update";
        } else {
            return "redirect:new";
        }
    }

    @PostMapping("update")
    public String processUpdate(@RequestParam(value = "id") Long id, ModelMap model,
                                @ModelAttribute("obj") @Valid EntityType obj, BindingResult result,
                                RedirectAttributes redirectAttributes)
    {
        if (result.hasErrors()) {
            setGenericInfo(model);
            model.put("newObj", false);
            return "admin/update";
        } else {
            obj.setId(id);
            entityRepository.save(obj);
            redirectAttributes.addFlashAttribute("successMessage", "Запись успешно обновлена!");
            return "redirect:.";
        }
    }

    @PostMapping("delete")
    public String processDelete(@RequestParam(value = "id") Long id, RedirectAttributes redirectAttributes) {
        try {
            entityRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Запись успешно удалена!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("warningMessage", "Ошибка: " + e.getLocalizedMessage());
        }

        return "redirect:.";
    }

    //endregion
}
