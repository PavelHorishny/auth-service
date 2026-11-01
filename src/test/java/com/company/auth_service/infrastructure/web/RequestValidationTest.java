package com.company.auth_service.infrastructure.web;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Annotation;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    // ---------- пароль: RegisterRequest ----------

    @Test
    void register_acceptsValidInput() {
        assertThat(validator.validate(new RegisterRequest("user@example.com", "password1"))).isEmpty();
    }

    @Test
    void register_acceptsPasswordWithSymbolsAndSpaces() {
        assertThat(validator.validate(new RegisterRequest("user@example.com", "my pass#word 1!"))).isEmpty();
    }

    @Test
    void register_acceptsPasswordOfExactlyMinLength() {
        assertThat(validator.validate(new RegisterRequest("user@example.com", "passwor1"))).isEmpty();   // 8
    }

    @Test
    void register_acceptsPasswordOfExactlyMaxLength() {
        String password = "a".repeat(71) + "1";                                                          // 72
        assertThat(validator.validate(new RegisterRequest("user@example.com", password))).isEmpty();
    }

    @Test
    void register_rejectsPasswordShorterThanMin() {
        assertThat(violations(new RegisterRequest("user@example.com", "passwo1"), "password", Pattern.class))   // 7
                .isNotEmpty();
    }

    @Test
    void register_rejectsPasswordLongerThanMax() {
        String password = "a".repeat(72) + "1";                                                          // 73
        assertThat(violations(new RegisterRequest("user@example.com", password), "password", Pattern.class))
                .isNotEmpty();
    }

    @Test
    void register_rejectsPasswordWithoutDigit() {
        assertThat(violations(new RegisterRequest("user@example.com", "onlyletters"), "password", Pattern.class))
                .isNotEmpty();
    }

    @Test
    void register_rejectsPasswordWithoutLetter() {
        assertThat(violations(new RegisterRequest("user@example.com", "12345678"), "password", Pattern.class))
                .isNotEmpty();
    }

    @Test
    void register_rejectsNonAsciiPassword() {
        assertThat(violations(new RegisterRequest("user@example.com", "пароль12345"), "password", Pattern.class))
                .isNotEmpty();
    }

    @Test
    void register_rejectsBlankPassword() {
        assertThat(violations(new RegisterRequest("user@example.com", "   "), "password", NotBlank.class))
                .isNotEmpty();
    }

    // ---------- email ----------

    @Test
    void register_rejectsBlankEmail() {
        assertThat(violations(new RegisterRequest("", "password1"), "email", NotBlank.class)).isNotEmpty();
    }

    @Test
    void register_rejectsMalformedEmail() {
        assertThat(violations(new RegisterRequest("not-an-email", "password1"), "email", Email.class)).isNotEmpty();
    }

    @Test
    void register_rejectsEmailLongerThan254() {
        String email = "a".repeat(255);                    // длина важна, формат здесь не проверяем
        assertThat(violations(new RegisterRequest(email, "password1"), "email", Size.class)).isNotEmpty();
    }

    @Test
    void register_doesNotRejectEmailOfExactly254ForLength() {
        String email = "a".repeat(254);
        assertThat(violations(new RegisterRequest(email, "password1"), "email", Size.class)).isEmpty();
    }

    @Test
    void resetPassword_rejectsEmailLongerThan254() {
        assertThat(violations(new ResetPasswordRequest("a".repeat(255)), "email", Size.class)).isNotEmpty();
    }

    @Test
    void resetPassword_acceptsValidEmail() {
        assertThat(validator.validate(new ResetPasswordRequest("user@example.com"))).isEmpty();
    }

    // ---------- ConfirmResetPasswordRequest ----------

    @Test
    void confirmReset_acceptsValidInput() {
        String token = "123e4567-e89b-12d3-a456-426614174000";                                           // 36
        assertThat(validator.validate(new ConfirmResetPasswordRequest(token, "newPassword1"))).isEmpty();
    }

    @Test
    void confirmReset_rejectsTokenLongerThan36() {
        assertThat(violations(new ConfirmResetPasswordRequest("t".repeat(37), "newPassword1"), "token", Size.class))
                .isNotEmpty();
    }

    @Test
    void confirmReset_rejectsBlankToken() {
        assertThat(violations(new ConfirmResetPasswordRequest(" ", "newPassword1"), "token", NotBlank.class))
                .isNotEmpty();
    }

    @Test
    void confirmReset_appliesTheSamePasswordRulesAsRegistration() {
        assertThat(violations(new ConfirmResetPasswordRequest("tok", "short1"), "newPassword", Pattern.class))
                .isNotEmpty();
        assertThat(violations(new ConfirmResetPasswordRequest("tok", "nodigitshere"), "newPassword", Pattern.class))
                .isNotEmpty();
    }

    // ---------- LoginRequest: правила сложности не проверяются, только размер ----------

    @Test
    void login_doesNotEnforcePasswordComplexity() {
        assertThat(validator.validate(new LoginRequest("user@example.com", "short"))).isEmpty();
    }

    @Test
    void login_rejectsOversizedPassword() {
        assertThat(violations(new LoginRequest("user@example.com", "a".repeat(129)), "password", Size.class))
                .isNotEmpty();
    }

    @Test
    void login_rejectsOversizedEmail() {
        assertThat(violations(new LoginRequest("a".repeat(255), "password1"), "email", Size.class)).isNotEmpty();
    }

    // ---------- helper ----------

    /** Нарушения одного свойства, вызванные именно этой аннотацией: тест падает по правильной причине. */
    private static <T> Set<ConstraintViolation<T>> violations(
            T request, String property, Class<? extends Annotation> constraint) {
        Set<ConstraintViolation<T>> all = validator.validateProperty(request, property);
        return all.stream()
                .filter(v -> v.getConstraintDescriptor().getAnnotation().annotationType().equals(constraint))
                .collect(java.util.stream.Collectors.toSet());
    }
}
