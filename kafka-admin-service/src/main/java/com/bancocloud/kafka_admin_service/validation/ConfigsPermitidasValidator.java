package com.bancocloud.kafka_admin_service.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Map;

public class ConfigsPermitidasValidator implements ConstraintValidator<ConfigsPermitidas, Map<String, String>> {

    private boolean permitirNulos;

    @Override
    public void initialize(ConfigsPermitidas anotacion) {
        this.permitirNulos = anotacion.permitirNulos();
    }

    @Override
    public boolean isValid(Map<String, String> configs, ConstraintValidatorContext context) {
        List<String> errores = ConfigsTopico.errores(configs, permitirNulos);
        if (errores.isEmpty()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        errores.forEach(e -> context.buildConstraintViolationWithTemplate(e.replace("{", "\\{"))
                .addConstraintViolation());
        return false;
    }
}
